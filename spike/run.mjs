// Phase 0 spike (#2): measures what the Meta Model API actually does for AfterBooth. Throwaway code.
//
//   node --env-file=<path to .env> spike/run.mjs <command> [--repeat N] [--input <id>] [--effort <level>] [--dry-run]
//   commands: models | spark-text | spark-image | spark-tools | spark-struct | sam | transcribe | all
//
// --dry-run builds every request and prints it without touching the network or reading the key.
// Live runs append one line per HTTP call to spike/out/calls.jsonl and save the raw exchange
// (images and audio replaced by sha256 + byte size) to spike/out/<capability>/<input>-<n>.json.
import { createHash, randomUUID } from 'node:crypto';
import { appendFileSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { parseArgs } from 'node:util';
import { printSummary, summarize } from './summarize.mjs';

const SPIKE_DIR = import.meta.dirname;
const SAMPLES = join(SPIKE_DIR, 'samples');
const OUT = join(SPIKE_DIR, 'out');

const { values: opts, positionals } = parseArgs({
  allowPositionals: true,
  options: {
    repeat: { type: 'string' },
    input: { type: 'string' },
    effort: { type: 'string' },
    // sam only: replace the labels.json phrase for one category, e.g. --phrase FOLDING_TABLE=table
    phrase: { type: 'string', multiple: true },
    'dry-run': { type: 'boolean', default: false },
    'no-lang-bias': { type: 'boolean', default: false },
  },
});
const DRY = opts['dry-run'];
const COMMAND = positionals[0];
if (!COMMAND) fail('usage: node spike/run.mjs <models|spark-text|spark-image|spark-tools|spark-struct|sam|transcribe|all> [--repeat N] [--input <id>] [--effort <level>] [--no-lang-bias] [--dry-run]');
const RUN_ID = new Date().toISOString().replace(/[:.]/g, '-');

// --- configuration: only from the environment, never guessed ---------------------------------
const ENV_NAMES = ['META_BASE_URL', 'META_SPARK_MODEL', 'META_SAM_MODEL', 'META_TRANSCRIBE_MODEL'];
const cfg = {};
for (const name of ENV_NAMES) {
  const value = process.env[name];
  if (!value && !DRY) fail(`${name} is not set. Run with node --env-file=<.env>.`);
  cfg[name] = value || `<${name}>`;
}
const API_BASE = cfg.META_BASE_URL.replace(/\/+$/, '').replace(/\/v1$/, '') + '/v1';
const apiKey = () => {
  const key = process.env.META_API_KEY;
  if (!key) fail('META_API_KEY is not set. Run with node --env-file=<.env>.');
  return key;
};

// Prices from https://dev.meta.ai/docs/pricing-rate-limits (Standard mode)
const PRICE = {
  sparkInput: 1.25 / 1e6,
  sparkCachedInput: 0.15 / 1e6,
  sparkOutput: 4.25 / 1e6,
  samImage: 2.5 / 1000,
  transcribeSecond: 0.18 / 3600,
};

const NOW = '2026-10-31T15:00:00+09:00';
const CATEGORIES = ['DISPLAY_RACK', 'STORAGE_BIN', 'FOLDING_TABLE'];
const VEHICLES = ['SEDAN', 'SUV', 'VAN', 'TRUCK_1T'];

const labels = JSON.parse(readFileSync(join(SAMPLES, 'labels.json'), 'utf8'));
const audio = JSON.parse(readFileSync(join(SAMPLES, 'audio.json'), 'utf8'));
const records = [];

// --- HTTP + recording ------------------------------------------------------------------------

/**
 * Sends one request, measures it, and returns a record that the caller completes with metrics and
 * then passes to `log`. In dry-run mode the request is printed and `stub` stands in for the response.
 */
async function send({ capability, inputId, n, method = 'POST', path, json, multipart, usage, stub }) {
  const url = API_BASE + path;
  const savedRequest = json ? redact(json) : multipart?.saved;
  const rec = { runId: RUN_ID, ts: new Date().toISOString(), capability, inputId, n, method, url, effort: opts.effort ?? null };
  if (DRY) {
    console.log(`\n[dry] ${capability} ${inputId}#${n}: ${method} ${url}`);
    console.log('[dry] headers: Authorization: Bearer <redacted>' + (multipart ? ', Content-Type: multipart/form-data' : json ? ', Content-Type: application/json' : ''));
    if (savedRequest) console.log('[dry] body:', truncate(JSON.stringify(savedRequest, null, 2), 1500));
    return { ...rec, dry: true, status: 0, ok: true, latencyMs: 0, body: stub?.() ?? null, usage: null, costUsd: 0 };
  }

  const headers = { Authorization: `Bearer ${apiKey()}`, Accept: 'application/json' };
  let body;
  if (json) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(json);
  } else if (multipart) {
    headers['Content-Type'] = `multipart/form-data; boundary=${multipart.boundary}`;
    body = multipart.body;
  }

  const started = performance.now();
  let status = 0;
  let text = '';
  let networkError = null;
  try {
    const res = await fetch(url, { method, headers, body, signal: AbortSignal.timeout(180_000) });
    status = res.status;
    text = await res.text();
  } catch (e) {
    networkError = `${e.name}: ${e.message}`;
  }
  const latencyMs = Math.round(performance.now() - started);
  let parsed = null;
  try {
    parsed = text ? JSON.parse(text) : null;
  } catch {
    parsed = null;
  }
  const ok = status >= 200 && status < 300;
  const u = ok && parsed ? usage(parsed) : null;
  Object.assign(rec, {
    status,
    ok,
    rateLimited: status === 429,
    latencyMs,
    usage: u,
    costUsd: u ? round6(u.costUsd) : 0,
    error: ok ? null : networkError ?? truncate(text, 2000),
    body: parsed,
  });

  const rawPath = join(OUT, capability, `${inputId}-${n}.json`);
  mkdirSync(join(OUT, capability), { recursive: true });
  writeFileSync(rawPath, JSON.stringify({ request: { method, url, body: savedRequest }, response: { status, latencyMs, body: parsed ? redact(parsed) : text } }, null, 2));
  rec.rawPath = rawPath.slice(SPIKE_DIR.length + 1).replaceAll('\\', '/');
  return rec;
}

function log(rec, metrics = null) {
  const { body, ...line } = rec;
  line.metrics = metrics;
  records.push(line);
  const tag = rec.dry ? 'DRY' : rec.rateLimited ? '429' : rec.ok ? 'ok' : 'FAIL';
  console.log(`[${rec.capability}] ${rec.inputId}#${rec.n} ${tag} ${rec.status} ${rec.latencyMs}ms $${(rec.costUsd ?? 0).toFixed(4)}` + (metrics?.score ? ` score ${metrics.score.hit}/${metrics.score.total}` : '') + (rec.error ? ` error: ${truncate(rec.error, 300)}` : ''));
  if (!rec.dry) {
    mkdirSync(OUT, { recursive: true });
    appendFileSync(join(OUT, 'calls.jsonl'), JSON.stringify(line) + '\n');
  }
}

// --- usage → cost --------------------------------------------------------------------------------

function chatUsage(json) {
  const u = json.usage ?? {};
  const input = u.prompt_tokens ?? 0;
  const cached = u.prompt_tokens_details?.cached_tokens ?? 0;
  const output = u.completion_tokens ?? 0;
  const reasoning = u.completion_tokens_details?.reasoning_tokens ?? null;
  return { inputTokens: input, cachedTokens: cached, outputTokens: output, reasoningTokens: reasoning, raw: u, costUsd: sparkCost(input, cached, output) };
}

function sparkCost(input, cached, output) {
  return (input - cached) * PRICE.sparkInput + cached * PRICE.sparkCachedInput + output * PRICE.sparkOutput;
}

function samUsage(json) {
  // SAM bills per image regardless of tokens; keep the raw usage object for the report.
  return { images: 1, raw: json.usage ?? null, costUsd: PRICE.samImage };
}

function transcribeUsage(json) {
  const seconds = Math.floor((json.audioDurationMs ?? 0) / 1000);
  return { audioSeconds: seconds, audioDurationMs: json.audioDurationMs ?? null, costUsd: seconds * PRICE.transcribeSecond };
}

// --- capabilities --------------------------------------------------------------------------------

async function models() {
  const rec = await send({
    capability: 'models',
    inputId: 'list',
    n: 1,
    method: 'GET',
    path: '/models',
    usage: () => ({ costUsd: 0 }),
    stub: () => ({ data: [] }),
  });
  const ids = (rec.body?.data ?? []).map((m) => m.id);
  const wanted = [cfg.META_SPARK_MODEL, cfg.META_SAM_MODEL, cfg.META_TRANSCRIBE_MODEL];
  const present = Object.fromEntries(wanted.map((id) => [id, ids.includes(id)]));
  if (!rec.dry) console.log('models:', ids.join(', '));
  log(rec, { models: ids, present, score: { hit: Object.values(present).filter(Boolean).length, total: wanted.length } });
}

async function sparkText() {
  const prompt = '높이 180cm 진열대가 왜 일반 승용차에 실리지 않는지 한국어 한 문장으로 설명해 주세요.';
  for (let n = 1; n <= repeat(5); n++) {
    const rec = await send({
      capability: 'spark-text',
      inputId: 'rack-sedan',
      n,
      path: '/chat/completions',
      json: withEffort({ model: cfg.META_SPARK_MODEL, messages: [{ role: 'user', content: prompt }] }),
      usage: chatUsage,
      stub: () => chatStub('높이 180cm라 승용차 트렁크와 뒷좌석에 들어가지 않아요.'),
    });
    const answer = rec.body?.choices?.[0]?.message?.content ?? '';
    const korean = /[가-힣]/.test(answer);
    log(rec, { answer, korean, chars: answer.length, finishReason: rec.body?.choices?.[0]?.finish_reason ?? null, score: { hit: rec.ok && korean ? 1 : 0, total: 1 } });
  }
}

const IMAGE_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['items'],
  properties: {
    items: {
      type: 'array',
      items: {
        type: 'object',
        additionalProperties: false,
        required: ['category', 'count', 'sizeCm', 'condition', 'confidence', 'boxes'],
        properties: {
          category: { type: 'string', enum: [...CATEGORIES, 'OTHER'] },
          count: { type: 'integer', minimum: 0 },
          sizeCm: {
            type: 'object',
            additionalProperties: false,
            required: ['width', 'depth', 'height'],
            properties: { width: { type: 'integer', minimum: 1 }, depth: { type: 'integer', minimum: 1 }, height: { type: 'integer', minimum: 1 } },
          },
          condition: { type: 'string', enum: ['GOOD', 'FAIR', 'POOR'] },
          confidence: { type: 'number', minimum: 0, maximum: 1 },
          boxes: {
            type: 'array',
            items: {
              type: 'object',
              additionalProperties: false,
              required: ['x_min', 'y_min', 'x_max', 'y_max'],
              properties: {
                x_min: { type: 'integer', minimum: 0, maximum: 1000 },
                y_min: { type: 'integer', minimum: 0, maximum: 1000 },
                x_max: { type: 'integer', minimum: 0, maximum: 1000 },
                y_max: { type: 'integer', minimum: 0, maximum: 1000 },
              },
            },
          },
        },
      },
    },
  },
};

const IMAGE_PROMPT = [
  'You are an object grounding expert helping register leftover event equipment.',
  'Find every display rack (shelving, gondola, rack for showing goods), storage bin (plastic box, tote, crate) and folding table in the image. Ensure the objects accurately match and do not miss any objects.',
  'Return one entry per category with: category (DISPLAY_RACK, STORAGE_BIN, FOLDING_TABLE, or OTHER for other furniture worth reusing), count (number of separate physical units), sizeCm (approximate size of one unit in centimeters: width, depth, height), condition (GOOD, FAIR or POOR), confidence (0-1, how sure you are about category, count and size), and boxes (one bounding box per unit as x_min, y_min, x_max, y_max on a normalized 0-1000 grid with (0,0) at the top-left).',
  'Omit categories that are not present.',
].join(' ');

async function sparkImage() {
  for (const photo of selected(labels.photos)) {
    const dataUrl = imageDataUrl(photo.file);
    for (let n = 1; n <= repeat(1); n++) {
      const rec = await send({
        capability: 'spark-image',
        inputId: photo.id,
        n,
        path: '/chat/completions',
        json: withEffort({
          model: cfg.META_SPARK_MODEL,
          messages: [{ role: 'user', content: [{ type: 'text', text: IMAGE_PROMPT }, { type: 'image_url', image_url: { url: dataUrl } }] }],
          response_format: { type: 'json_schema', json_schema: { name: 'IntakeEstimate', schema: IMAGE_SCHEMA } },
        }),
        usage: chatUsage,
        stub: () => chatStub(JSON.stringify({ items: photo.items.map((i) => ({ category: i.category, count: i.count, sizeCm: i.approxSizeCm, condition: 'GOOD', confidence: 0.9, boxes: [] })) })),
      });
      log(rec, rec.ok ? scoreImage(photo, rec.body?.choices?.[0]?.message?.content) : null);
    }
  }
}

function scoreImage(photo, content) {
  const { value, error } = parseJson(content);
  const schemaErrors = error ? [error] : validate(IMAGE_SCHEMA, value);
  const items = value?.items ?? [];
  const predicted = {};
  for (const item of items) predicted[item.category] = (predicted[item.category] ?? 0) + (item.count ?? 0);
  const perCategory = [];
  let hit = 0;
  let total = 0;
  for (const truth of photo.items) {
    const got = predicted[truth.category] ?? 0;
    const inRange = got >= truth.countMin && got <= truth.countMax;
    const item = items.find((i) => i.category === truth.category);
    perCategory.push({
      category: truth.category,
      expected: truth.count,
      range: [truth.countMin, truth.countMax],
      predicted: got,
      exact: got === truth.count,
      inRange,
      boxes: item?.boxes?.length ?? 0,
      sizeCm: item?.sizeCm ?? null,
      expectedSizeCm: truth.approxSizeCm,
      condition: item?.condition ?? null,
      confidence: item?.confidence ?? null,
    });
    total++;
    if (inRange) hit++;
  }
  const falsePositives = CATEGORIES.filter((c) => !photo.items.some((t) => t.category === c) && (predicted[c] ?? 0) > 0);
  total += falsePositives.length;
  return { schemaValid: schemaErrors.length === 0, schemaErrors, perCategory, falsePositives, other: predicted.OTHER ?? 0, score: { hit, total } };
}

// Stub domain data for the allocation agent loop. Cargo sizes are spike placeholders, not product constants.
const STUB_ITEMS = [
  { itemId: 101, category: 'DISPLAY_RACK', label: '진열대', sizeCm: { width: 90, depth: 40, height: 180 }, condition: 'GOOD', availableQty: 2, pickupDeadline: '2026-10-31T20:00:00+09:00' },
  { itemId: 102, category: 'DISPLAY_RACK', label: '진열대', sizeCm: { width: 120, depth: 45, height: 160 }, condition: 'FAIR', availableQty: 1, pickupDeadline: '2026-11-01T18:00:00+09:00' },
  { itemId: 201, category: 'FOLDING_TABLE', label: '접이식 테이블', sizeCm: { width: 120, depth: 60, height: 8 }, condition: 'GOOD', availableQty: 3, pickupDeadline: '2026-11-01T18:00:00+09:00' },
  { itemId: 301, category: 'STORAGE_BIN', label: '수납함', sizeCm: { width: 60, depth: 40, height: 35 }, condition: 'FAIR', availableQty: 12, pickupDeadline: '2026-11-01T12:00:00+09:00' },
];
const STUB_CARGO = { SEDAN: [45, 90, 130], SUV: [80, 110, 180], VAN: [130, 150, 250], TRUCK_1T: [160, 200, 280] };
const STUB_REQUEST = { requestId: 7, category: 'DISPLAY_RACK', quantity: 2, vehicleType: 'SEDAN', purpose: '주말 행사 진열용' };
const DIMENSION_KO = { width: '폭', depth: '깊이', height: '높이' };

const TOOL_SCHEMAS = {
  searchCandidates: {
    type: 'object',
    additionalProperties: false,
    required: ['category'],
    properties: {
      category: { type: 'string', enum: CATEGORIES, description: 'Item category to search' },
      minQuantity: { type: 'integer', minimum: 1, description: 'Only items with at least this many available' },
    },
  },
  checkVehicleFit: {
    type: 'object',
    additionalProperties: false,
    required: ['itemId', 'vehicleType'],
    properties: { itemId: { type: 'integer' }, vehicleType: { type: 'string', enum: VEHICLES } },
  },
  holdQuantity: {
    type: 'object',
    additionalProperties: false,
    required: ['requestId', 'itemId', 'quantity'],
    properties: { requestId: { type: 'integer' }, itemId: { type: 'integer' }, quantity: { type: 'integer', minimum: 1 } },
  },
};
const TOOL_DESCRIPTIONS = {
  searchCandidates: 'Search available inventory items of one category whose pickup deadline has not passed. Returns item id, size in cm, condition, available quantity and pickup deadline.',
  checkVehicleFit: "Check on the server whether one item fits the given vehicle's cargo space. Returns fits, a reason code and the limiting dimension.",
  holdQuantity: 'Ask the server to hold a quantity of one item for the pickup request (status HELD, expires in 15 minutes). The server validates availability, deadline, vehicle fit, category and the request quantity, and may refuse.',
};
const TOOLS = Object.keys(TOOL_SCHEMAS).map((name) => ({ type: 'function', function: { name, description: TOOL_DESCRIPTIONS[name], parameters: TOOL_SCHEMAS[name] } }));

function vehicleFit(item, vehicleType) {
  const dims = Object.entries(item.sizeCm).sort((a, b) => a[1] - b[1]);
  const cargo = STUB_CARGO[vehicleType];
  const tooBig = dims.findIndex(([, v], i) => v > cargo[i]);
  if (tooBig === -1) return { fits: true, code: null };
  const [dim, cm] = dims[tooBig];
  return { fits: false, code: 'VEHICLE_TOO_SMALL', limitingDimension: dim, message: `${DIMENSION_KO[dim]} ${cm}cm라 ${vehicleType === 'SEDAN' ? '승용차' : vehicleType}에 실을 수 없어요.` };
}

function runTool(name, args, state) {
  if (name === 'searchCandidates') {
    return { items: STUB_ITEMS.filter((i) => i.category === args.category && i.availableQty >= (args.minQuantity ?? 1)) };
  }
  const item = STUB_ITEMS.find((i) => i.itemId === args.itemId);
  if (!item) return { outcome: 'REJECTED', code: 'NOT_FOUND', message: '없는 물품이에요.' };
  if (name === 'checkVehicleFit') return { itemId: item.itemId, vehicleType: args.vehicleType, ...vehicleFit(item, args.vehicleType) };
  if (name === 'holdQuantity') {
    if (args.requestId !== STUB_REQUEST.requestId) return { outcome: 'REJECTED', code: 'NOT_FOUND', message: '없는 요청이에요.' };
    if (item.category !== STUB_REQUEST.category) return { outcome: 'REJECTED', code: 'CATEGORY_MISMATCH', message: '요청한 품목만 자동으로 잡을 수 있어요.' };
    const fit = vehicleFit(item, STUB_REQUEST.vehicleType);
    if (!fit.fits) return { outcome: 'REJECTED', code: fit.code, message: fit.message };
    if (args.quantity > item.availableQty) return { outcome: 'REJECTED', code: 'QTY_UNAVAILABLE', message: '남은 수량이 부족해요.' };
    if (state.held + args.quantity > STUB_REQUEST.quantity) return { outcome: 'REJECTED', code: 'QTY_EXCEEDS_REQUEST', message: '요청 수량을 넘어요.' };
    state.held += args.quantity;
    return { outcome: 'OK', reservationId: 900 + state.held, status: 'HELD', holdExpiresAt: '2026-10-31T15:15:00+09:00' };
  }
  return { outcome: 'REJECTED', code: 'UNKNOWN_TOOL' };
}

const AGENT_SYSTEM = [
  'You are the AfterBooth allocation agent. You match a pickup request with leftover event equipment by calling the server tools.',
  'Rules: the server decides; never claim a hold that holdQuantity did not return as OK. Only hold items of the requested category, never more than the requested quantity.',
  'Check vehicle fit before holding. If nothing of the requested category can be held, hold nothing, search other categories that fit the vehicle, and suggest one as an alternative.',
  'Finish with a short answer to the receiver in Korean (one or two sentences): what was held, or why it does not fit and the alternative.',
  `Pickup request: ${JSON.stringify(STUB_REQUEST)}. Current time: ${NOW}.`,
].join('\n');

async function sparkTools() {
  const transcript = '주말 행사용 진열대 2개, 승용차밖에 없어요';
  for (let run = 1; run <= repeat(5); run++) {
    const messages = [
      { role: 'system', content: AGENT_SYSTEM },
      { role: 'user', content: transcript },
    ];
    const state = { held: 0, calls: [] };
    const sequence = [];
    let wellFormed = true;
    let schemaValid = true;
    let finalText = null;
    for (let turn = 1; turn <= 6; turn++) {
      const rec = await send({
        capability: 'spark-tools',
        inputId: `run${run}-turn${turn}`,
        n: 1,
        path: '/chat/completions',
        json: withEffort({ model: cfg.META_SPARK_MODEL, messages, tools: TOOLS }),
        usage: chatUsage,
        stub: () => toolStub(turn),
      });
      const msg = rec.body?.choices?.[0]?.message;
      const calls = msg?.tool_calls ?? [];
      const turnCalls = [];
      if (rec.ok && calls.length > 0) {
        messages.push({ role: 'assistant', content: msg.content ?? '', tool_calls: calls });
        for (const call of calls) {
          const { value: args, error } = parseJson(call.function?.arguments);
          const argErrors = error ? [error] : validate(TOOL_SCHEMAS[call.function?.name] ?? {}, args);
          if (error) wellFormed = false;
          if (argErrors.length) schemaValid = false;
          const result = error ? { outcome: 'REJECTED', code: 'INVALID_ARGUMENTS', message: error } : runTool(call.function.name, args, state);
          sequence.push(call.function?.name);
          state.calls.push({ name: call.function?.name, args, outcome: result.outcome ?? 'OK', code: result.code ?? null });
          turnCalls.push({ name: call.function?.name, arguments: call.function?.arguments, argErrors, result });
          messages.push({ role: 'tool', tool_call_id: call.id, content: JSON.stringify(result) });
        }
      } else if (rec.ok) {
        finalText = msg?.content ?? '';
      }
      const done = !rec.ok || calls.length === 0 || turn === 6;
      let metrics = { toolCalls: turnCalls, finishReason: rec.body?.choices?.[0]?.finish_reason ?? null };
      if (done) {
        const korean = /[가-힣]/.test(finalText ?? '');
        const offersAlternative = /접이식|테이블|수납함|201|301/.test(finalText ?? '');
        // No display rack fits a sedan in the stub data, so any holdQuantity call is one the server must refuse.
        const holdAttempts = state.calls.filter((c) => c.name === 'holdQuantity');
        const searchedOther = state.calls.some((c) => c.name === 'searchCandidates' && c.args?.category !== STUB_REQUEST.category);
        const passed = rec.ok && wellFormed && schemaValid && korean && offersAlternative && holdAttempts.length === 0;
        metrics = { ...metrics, run, turns: turn, sequence, wellFormed, schemaValid, holdAttempts, searchedOther, finalText, korean, offersAlternative, score: { hit: passed ? 1 : 0, total: 1 } };
      }
      log(rec, metrics);
      if (done) break;
    }
  }
}

const CONDITIONS_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['conditions', 'missingFields'],
  properties: {
    conditions: {
      type: 'object',
      additionalProperties: false,
      properties: {
        category: { type: 'string', enum: CATEGORIES },
        quantity: { type: 'integer', minimum: 1 },
        vehicleType: { type: 'string', enum: VEHICLES },
        pickupDeadline: { type: 'string', format: 'date-time' },
        purpose: { type: 'string' },
      },
    },
    missingFields: { type: 'array', items: { type: 'string', enum: ['category', 'quantity', 'vehicleType', 'pickupDeadline', 'purpose'] } },
  },
};

// The contract's ConditionInterpretation; the server adds context and transcript around Spark's output.
const INTERPRETATION_SCHEMA = {
  type: 'object',
  additionalProperties: false,
  required: ['context', 'transcript', 'conditions', 'missingFields'],
  properties: {
    context: { type: 'string', enum: ['GIVER', 'RECEIVER'] },
    transcript: { type: 'string' },
    conditions: CONDITIONS_SCHEMA.properties.conditions,
    missingFields: CONDITIONS_SCHEMA.properties.missingFields,
  },
};

function structPrompt(context, transcript) {
  return [
    'Turn a spoken Korean equipment hand-off condition into JSON.',
    context === 'GIVER'
      ? 'Speaker: GIVER, who has leftover items. pickupDeadline = the time the items must be picked up by.'
      : 'Speaker: RECEIVER, who wants items. pickupDeadline = the time the receiver can pick up by.',
    'Fill only what was said; omit every other field (never null). Categories: DISPLAY_RACK (진열대, 선반, 랙), STORAGE_BIN (수납함, 박스, 바구니), FOLDING_TABLE (접이식 테이블).',
    'vehicleType: SEDAN (승용차), SUV, VAN (승합차, 밴), TRUCK_1T (1톤 트럭). "X 이상" means the smallest vehicle X.',
    `Resolve relative times in Asia/Seoul against now = ${NOW} (Saturday) and write them as ISO 8601 with the +09:00 offset.`,
    'missingFields: for RECEIVER list category and vehicleType if they were not said; for GIVER return an empty list.',
    `Transcript: ${transcript}`,
  ].join('\n');
}

async function structure({ capability, inputId, n, context, transcript, expected }) {
  const rec = await send({
    capability,
    inputId,
    n,
    path: '/chat/completions',
    json: withEffort({
      model: cfg.META_SPARK_MODEL,
      messages: [{ role: 'user', content: structPrompt(context, transcript) }],
      response_format: { type: 'json_schema', json_schema: { name: 'ConditionInterpretation', schema: CONDITIONS_SCHEMA } },
    }),
    usage: chatUsage,
    stub: () => chatStub(JSON.stringify({ conditions: expected, missingFields: [] })),
  });
  if (!rec.ok) return log(rec);
  const { value, error } = parseJson(rec.body?.choices?.[0]?.message?.content);
  const interpretation = value ? { context, transcript, ...value } : null;
  const schemaErrors = error ? [error] : validate(INTERPRETATION_SCHEMA, interpretation);
  const got = value?.conditions ?? {};
  const fields = Object.entries(expected).map(([key, want]) => {
    const have = got[key];
    const equal = key === 'pickupDeadline' ? typeof have === 'string' && Date.parse(have) === Date.parse(want) : have === want;
    return { key, want, have: have ?? null, equal };
  });
  const extra = Object.keys(got).filter((k) => !(k in expected));
  const hit = fields.filter((f) => f.equal).length + (schemaErrors.length === 0 ? 1 : 0);
  log(rec, { interpretation, schemaValid: schemaErrors.length === 0, schemaErrors, fields, extraFields: extra, score: { hit, total: fields.length + 1 } });
}

async function sparkStruct() {
  for (const clip of selected(audio.clips)) {
    for (let n = 1; n <= repeat(1); n++) {
      await structure({ capability: 'spark-struct', inputId: clip.id, n, context: clip.context, transcript: clip.text, expected: clip.expected });
    }
  }
}

async function sam() {
  for (const photo of selected(labels.photos)) {
    const dataUrl = imageDataUrl(photo.file);
    const overrides = Object.fromEntries((opts.phrase ?? []).map((p) => p.split('=')));
    const phrases = opts.phrase ? overrides : labels.samPhrases;
    for (const [category, phrase] of Object.entries(phrases)) {
      for (let n = 1; n <= repeat(1); n++) {
        const rec = await send({
          capability: 'sam',
          inputId: opts.phrase ? `${photo.id}--${category}--${phrase.replace(/\W+/g, '-')}` : `${photo.id}--${category}`,
          n,
          path: '/responses',
          json: {
            model: cfg.META_SAM_MODEL,
            input: [{ type: 'message', role: 'user', content: [{ type: 'input_text', text: phrase }, { type: 'input_image', image_url: dataUrl }] }],
            metadata: { include_confidence: 'true', mask_encoding: 'one_bit' },
          },
          usage: samUsage,
          stub: () => ({ output: [{ type: 'message', content: [{ type: 'output_text', text: `<0f>0<|box;x1=10;y1=20;x2=110;y2=220;w=${photo.width};h=${photo.height};c=0.91|><|mask;x=0;y=0;c=0.91;data=2,2,!AAAA|>` }] }] }),
        });
        if (!rec.ok) {
          log(rec);
          continue;
        }
        const text = responsesText(rec.body);
        const parsed = parseSam(text);
        const truth = photo.items.find((i) => i.category === category);
        const range = truth ? [truth.countMin, truth.countMax] : [0, 0];
        const inRange = parsed.count >= range[0] && parsed.count <= range[1];
        const count50 = parsed.records.filter((r) => (r.c ?? 1) >= 0.5).length;
        log(rec, {
          phrase,
          expected: truth?.count ?? 0,
          range,
          count: parsed.count,
          count50,
          exact: parsed.count === (truth?.count ?? 0),
          inRange,
          inRange50: count50 >= range[0] && count50 <= range[1],
          confidences: parsed.records.map((r) => r.c),
          boxes: parsed.records.map((r) => r.box),
          maskBytes: parsed.maskBytes,
          outputChars: text.length,
          frameSize: parsed.frameSize,
          score: { hit: inRange ? 1 : 0, total: 1 },
        });
      }
    }
  }
}

/** Spark finds and counts, SAM masks: Spark's 0–1000 boxes from the last spark-image run become SAM box prompts. */
async function samBoxes() {
  for (const photo of selected(labels.photos)) {
    const rawPath = join(OUT, 'spark-image', `${photo.id}-1.json`);
    const saved = DRY ? null : JSON.parse(readFileSync(rawPath, 'utf8'));
    const items = saved ? JSON.parse(saved.response.body.choices[0].message.content).items : photo.items.map((i) => ({ boxes: [{ x_min: 100, y_min: 100, x_max: 900, y_max: 900 }] }));
    const { width: w, height: h } = photo;
    const sparkBoxes = items
      .flatMap((i) => i.boxes ?? [])
      .slice(0, 16)
      .map((b) => ({
        x1: Math.floor((b.x_min / 1000) * (w - 1)),
        y1: Math.floor((b.y_min / 1000) * (h - 1)),
        x2: Math.floor((b.x_max / 1000) * (w - 1)),
        y2: Math.floor((b.y_max / 1000) * (h - 1)),
      }));
    const prompt = '<0f>' + sparkBoxes.map((b, i) => `${i + 1}<|box;x1=${b.x1};y1=${b.y1};x2=${b.x2};y2=${b.y2};w=${w};h=${h}|>`).join(',');
    const rec = await send({
      capability: 'sam-boxes',
      inputId: photo.id,
      n: 1,
      path: '/responses',
      json: {
        model: cfg.META_SAM_MODEL,
        input: [{ type: 'message', role: 'user', content: [{ type: 'input_text', text: prompt }, { type: 'input_image', image_url: imageDataUrl(photo.file) }] }],
        metadata: { include_confidence: 'true', mask_encoding: 'one_bit' },
      },
      usage: samUsage,
      stub: () => ({ output: [{ type: 'message', content: [{ type: 'output_text', text: `<0f>1<|box;x1=10;y1=20;x2=110;y2=220;w=${w};h=${h};c=0.91|><|mask;x=0;y=0;c=0.91;data=2,2,!AAAA|>` }] }] }),
    });
    if (!rec.ok) {
      log(rec);
      continue;
    }
    const parsed = parseSam(responsesText(rec.body));
    const ious = parsed.records.map((r) => round6(iou(r.box, sparkBoxes[r.id - 1] ?? r.box)));
    log(rec, {
      sent: sparkBoxes.length,
      returned: parsed.count,
      confidences: parsed.records.map((r) => r.c),
      iouWithSparkBox: ious,
      maskBytes: parsed.maskBytes,
      score: { hit: parsed.count === sparkBoxes.length ? 1 : 0, total: 1 },
    });
  }
}

function iou(a, b) {
  const ix = Math.max(0, Math.min(a.x2, b.x2) - Math.max(a.x1, b.x1));
  const iy = Math.max(0, Math.min(a.y2, b.y2) - Math.max(a.y1, b.y1));
  const area = (r) => (r.x2 - r.x1) * (r.y2 - r.y1);
  return (ix * iy) / (area(a) + area(b) - ix * iy);
}

async function transcribe() {
  for (const clip of selected(audio.clips)) {
    const wav = readFileSync(join(SAMPLES, clip.file));
    for (let n = 1; n <= repeat(1); n++) {
      const request = { model: cfg.META_TRANSCRIBE_MODEL, audioEncoding: 'WAV', mode: 'PUSH_TO_TALK' };
      if (!opts['no-lang-bias']) request.languageBias = ['Korean'];
      const rec = await send({
        capability: 'transcribe',
        inputId: clip.id,
        n,
        path: '/asr/transcribe',
        multipart: buildMultipart(request, wav, clip.file),
        usage: transcribeUsage,
        stub: () => ({ transcript: clip.text, audioDurationMs: 5000, turns: [] }),
      });
      if (!rec.ok) {
        log(rec);
        continue;
      }
      const transcript = rec.body?.transcript ?? '';
      const cer = charErrorRate(clip.text, transcript);
      log(rec, { reference: clip.text, transcript, cer: round6(cer), audioDurationMs: rec.body?.audioDurationMs ?? null, score: { hit: cer <= 0.1 ? 1 : 0, total: 1 } });
      await structure({ capability: 'spark-struct-chain', inputId: clip.id, n, context: clip.context, transcript, expected: clip.expected });
    }
  }
}

// --- helpers -----------------------------------------------------------------------------------

function buildMultipart(request, wav, filename) {
  const boundary = `----afterbooth-${randomUUID()}`;
  const head =
    `--${boundary}\r\nContent-Disposition: form-data; name="request"\r\nContent-Type: application/json\r\n\r\n${JSON.stringify(request)}\r\n` +
    `--${boundary}\r\nContent-Disposition: form-data; name="audio"; filename="${filename}"\r\nContent-Type: audio/wav\r\n\r\n`;
  const body = Buffer.concat([Buffer.from(head, 'utf8'), wav, Buffer.from(`\r\n--${boundary}--\r\n`, 'utf8')]);
  return { boundary, body, saved: { request, audio: { file: filename, sha256: sha256(wav), bytes: wav.length } } };
}

function responsesText(body) {
  if (typeof body?.output_text === 'string') return body.output_text;
  return (body?.output ?? [])
    .flatMap((o) => o.content ?? [])
    .filter((c) => c.type === 'output_text')
    .map((c) => c.text)
    .join('');
}

/** Parses SAM special-token output (grammar in docs sam/reading-segmentation): boxes, confidence, mask size. */
function parseSam(text) {
  const records = [];
  let maskBytes = 0;
  let frameSize = null;
  const recordRe = /(\d+)<\|box;([^|]*)\|><\|mask;([^|]*?)data=([^|]*)\|>/g;
  for (const m of text.matchAll(recordRe)) {
    const box = Object.fromEntries(m[2].split(';').map((kv) => kv.split('=')).map(([k, v]) => [k, Number(v)]));
    const maskFields = Object.fromEntries(m[3].split(';').filter(Boolean).map((kv) => kv.split('=')).map(([k, v]) => [k, Number(v)]));
    frameSize ??= { w: box.w, h: box.h };
    maskBytes += m[4].length;
    records.push({ id: Number(m[1]), c: box.c ?? maskFields.c ?? null, box: { x1: box.x1, y1: box.y1, x2: box.x2, y2: box.y2 } });
  }
  const boxTokens = (text.match(/<\|box;/g) ?? []).length;
  return { count: boxTokens, parsedRecords: records.length, records, maskBytes, frameSize };
}

/** Character error rate on text with whitespace and punctuation removed. */
function charErrorRate(reference, hypothesis) {
  const norm = (s) => [...s.toLowerCase().replace(/[\s\p{P}\p{S}]/gu, '')];
  const r = norm(reference);
  const h = norm(hypothesis);
  if (r.length === 0) return h.length === 0 ? 0 : 1;
  let prev = Array.from({ length: h.length + 1 }, (_, j) => j);
  for (let i = 1; i <= r.length; i++) {
    const cur = [i];
    for (let j = 1; j <= h.length; j++) cur[j] = Math.min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (r[i - 1] === h[j - 1] ? 0 : 1));
    prev = cur;
  }
  return prev[h.length] / r.length;
}

/** Minimal JSON Schema check: type, enum, required, properties, additionalProperties:false, items, minimum, maximum, format date-time. */
function validate(schema, value, path = '$') {
  const errors = [];
  if (value === null || value === undefined) return [`${path}: is ${value}`];
  if (schema.enum && !schema.enum.includes(value)) errors.push(`${path}: ${JSON.stringify(value)} not in enum`);
  switch (schema.type) {
    case 'object':
      if (typeof value !== 'object' || Array.isArray(value)) return [`${path}: not an object`];
      for (const key of schema.required ?? []) if (!(key in value)) errors.push(`${path}.${key}: missing`);
      for (const [key, v] of Object.entries(value)) {
        const sub = schema.properties?.[key];
        if (sub) errors.push(...validate(sub, v, `${path}.${key}`));
        else if (schema.additionalProperties === false) errors.push(`${path}.${key}: not allowed`);
      }
      break;
    case 'array':
      if (!Array.isArray(value)) return [`${path}: not an array`];
      value.forEach((v, i) => errors.push(...validate(schema.items ?? {}, v, `${path}[${i}]`)));
      break;
    case 'integer':
    case 'number':
      if (typeof value !== 'number' || (schema.type === 'integer' && !Number.isInteger(value))) return [`${path}: not ${schema.type}`];
      if (schema.minimum !== undefined && value < schema.minimum) errors.push(`${path}: < ${schema.minimum}`);
      if (schema.maximum !== undefined && value > schema.maximum) errors.push(`${path}: > ${schema.maximum}`);
      break;
    case 'string':
      if (typeof value !== 'string') return [`${path}: not a string`];
      if (schema.format === 'date-time' && !/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:\d{2})$/.test(value)) errors.push(`${path}: not ISO 8601 date-time`);
      break;
  }
  return errors;
}

function parseJson(text) {
  if (typeof text !== 'string') return { value: null, error: 'no text content' };
  try {
    return { value: JSON.parse(text), error: null };
  } catch (e) {
    return { value: null, error: `invalid JSON: ${e.message}` };
  }
}

/** Replaces every data: URL with its sha256 and size so images and audio never land in saved files. */
function redact(value) {
  if (typeof value === 'string') {
    if (!value.startsWith('data:')) return value;
    const comma = value.indexOf(',');
    const bytes = Buffer.from(value.slice(comma + 1), 'base64');
    return { redactedDataUrl: value.slice(0, comma), sha256: sha256(bytes), bytes: bytes.length };
  }
  if (Array.isArray(value)) return value.map(redact);
  if (value && typeof value === 'object') return Object.fromEntries(Object.entries(value).map(([k, v]) => [k, redact(v)]));
  return value;
}

function imageDataUrl(file) {
  const mime = file.endsWith('.png') ? 'image/png' : 'image/jpeg';
  return `data:${mime};base64,${readFileSync(join(SAMPLES, file)).toString('base64')}`;
}

function withEffort(body) {
  return opts.effort ? { ...body, reasoning_effort: opts.effort } : body;
}

function chatStub(content) {
  return { choices: [{ message: { role: 'assistant', content }, finish_reason: 'stop' }], usage: null };
}

function toolStub(turn) {
  const call = (id, name, args) => ({ id, type: 'function', function: { name, arguments: JSON.stringify(args) } });
  const steps = [
    [call('c1', 'searchCandidates', { category: 'DISPLAY_RACK' })],
    [call('c2', 'checkVehicleFit', { itemId: 101, vehicleType: 'SEDAN' }), call('c3', 'checkVehicleFit', { itemId: 102, vehicleType: 'SEDAN' })],
    [call('c4', 'searchCandidates', { category: 'FOLDING_TABLE' })],
  ];
  if (turn <= steps.length) return { choices: [{ message: { role: 'assistant', content: null, tool_calls: steps[turn - 1] }, finish_reason: 'tool_calls' }] };
  return chatStub('높이 180cm라 승용차에 실을 수 없어요. 대신 접이식 테이블 2개 어떠세요?');
}

function selected(list) {
  return opts.input ? list.filter((x) => x.id === opts.input) : list;
}

function repeat(defaultN) {
  return opts.repeat ? Number(opts.repeat) : defaultN;
}

function sha256(buf) {
  return createHash('sha256').update(buf).digest('hex');
}

function truncate(s, max) {
  return s.length > max ? `${s.slice(0, max)}… (${s.length} chars)` : s;
}

function round6(x) {
  return Math.round(x * 1e6) / 1e6;
}

function fail(message) {
  console.error(message);
  process.exit(1);
}

// --- main --------------------------------------------------------------------------------------

const COMMANDS = {
  models,
  'spark-text': sparkText,
  'spark-image': sparkImage,
  'spark-tools': sparkTools,
  'spark-struct': sparkStruct,
  sam,
  'sam-boxes': samBoxes,
  transcribe,
};

if (!COMMAND || !(COMMAND in COMMANDS || COMMAND === 'all')) {
  fail(`usage: node spike/run.mjs <${[...Object.keys(COMMANDS), 'all'].join('|')}> [--repeat N] [--input <id>] [--effort <level>] [--no-lang-bias] [--dry-run]`);
}
console.log(`run ${RUN_ID}${DRY ? ' (DRY RUN: no network, no key, stub responses only)' : ''} → ${API_BASE}`);
for (const name of COMMAND === 'all' ? Object.keys(COMMANDS) : [COMMAND]) await COMMANDS[name]();
printSummary(summarize(records), { dry: DRY });
