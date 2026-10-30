// Aggregates spike/out/calls.jsonl into spike/out/summary.json and a Markdown table for spike/REPORT.md.
//
//   node spike/summarize.mjs            all live runs
//   node spike/summarize.mjs --last     only the most recent run
//   node spike/summarize.mjs --run <id> one run (the runId printed by run.mjs)
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { parseArgs } from 'node:util';
import { pathToFileURL } from 'node:url';

export function summarize(records) {
  const byCapability = new Map();
  for (const r of records) {
    if (!byCapability.has(r.capability)) byCapability.set(r.capability, []);
    byCapability.get(r.capability).push(r);
  }
  return [...byCapability].map(([capability, rs]) => {
    const ok = rs.filter((r) => r.ok);
    const latencies = ok.map((r) => r.latencyMs).sort((a, b) => a - b);
    const scored = rs.map((r) => r.metrics?.score).filter(Boolean);
    const hit = scored.reduce((s, x) => s + x.hit, 0);
    const total = scored.reduce((s, x) => s + x.total, 0);
    const cers = rs.map((r) => r.metrics?.cer).filter((x) => typeof x === 'number');
    const tokenCalls = ok.filter((r) => typeof r.usage?.inputTokens === 'number');
    const cost = rs.reduce((s, r) => s + (r.costUsd ?? 0), 0);
    return {
      capability,
      calls: rs.length,
      ok: ok.length,
      rateLimited: rs.filter((r) => r.rateLimited).length,
      p50Ms: percentile(latencies, 0.5),
      maxMs: latencies.at(-1) ?? null,
      accuracy: total ? { hit, total, ratio: round(hit / total, 3) } : null,
      meanCer: cers.length ? round(cers.reduce((a, b) => a + b, 0) / cers.length, 4) : null,
      avgInputTokens: tokenCalls.length ? Math.round(tokenCalls.reduce((s, r) => s + r.usage.inputTokens, 0) / tokenCalls.length) : null,
      avgOutputTokens: tokenCalls.length ? Math.round(tokenCalls.reduce((s, r) => s + r.usage.outputTokens, 0) / tokenCalls.length) : null,
      costUsd: round(cost, 6),
      costPerCallUsd: rs.length ? round(cost / rs.length, 6) : null,
    };
  });
}

export function printSummary(rows, { dry = false } = {}) {
  if (dry) console.log('\nDRY RUN: no calls were made. The table only shows how many requests a live run would send; latency, accuracy and cost are stub values.');
  const header = ['capability', 'calls', 'ok', '429', 'p50 ms', 'max ms', 'accuracy', 'mean CER', 'avg tok in/out', 'cost $', 'cost/call $'];
  const lines = [`| ${header.join(' | ')} |`, `|${header.map(() => '---').join('|')}|`];
  for (const r of rows) {
    lines.push(
      `| ${[
        r.capability,
        r.calls,
        r.ok,
        r.rateLimited,
        r.p50Ms ?? '-',
        r.maxMs ?? '-',
        r.accuracy ? `${r.accuracy.hit}/${r.accuracy.total} (${Math.round(r.accuracy.ratio * 100)}%)` : '-',
        r.meanCer ?? '-',
        r.avgInputTokens === null ? '-' : `${r.avgInputTokens}/${r.avgOutputTokens}`,
        r.costUsd.toFixed(4),
        r.costPerCallUsd?.toFixed(5) ?? '-',
      ].join(' | ')} |`,
    );
  }
  const totalCalls = rows.reduce((s, r) => s + r.calls, 0);
  const totalCost = rows.reduce((s, r) => s + r.costUsd, 0);
  lines.push('', `Total: ${totalCalls} calls, $${totalCost.toFixed(4)}`);
  console.log('\n' + lines.join('\n'));
  return lines.join('\n');
}

function percentile(sorted, p) {
  if (!sorted.length) return null;
  return sorted[Math.min(sorted.length - 1, Math.ceil(p * sorted.length) - 1)];
}

function round(x, digits) {
  const f = 10 ** digits;
  return Math.round(x * f) / f;
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  const { values } = parseArgs({ options: { last: { type: 'boolean', default: false }, run: { type: 'string' } } });
  const out = join(import.meta.dirname, 'out');
  const file = join(out, 'calls.jsonl');
  if (!existsSync(file)) {
    console.error(`No ${file}. Run spike/run.mjs live first.`);
    process.exit(1);
  }
  let records = readFileSync(file, 'utf8').split('\n').filter(Boolean).map((l) => JSON.parse(l));
  const runId = values.run ?? (values.last ? records.at(-1)?.runId : null);
  if (runId) records = records.filter((r) => r.runId === runId);
  const rows = summarize(records);
  const markdown = printSummary(rows);
  writeFileSync(join(out, 'summary.json'), JSON.stringify({ generatedAt: new Date().toISOString(), runId: runId ?? 'all', rows, markdown }, null, 2));
  console.log(`\nwrote ${join(out, 'summary.json')}`);
}
