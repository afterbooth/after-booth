// Runs a command with the environment Node loaded from --env-file, so tools without an env-file option
// (Gradle) see META_* without anyone reading .env by hand. Prints nothing from the environment.
//
//   node --env-file=<path to .env> spike/with-env.mjs <command> [args...]
import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';

const [command, ...args] = process.argv.slice(2);
if (!command) {
  console.error('usage: node --env-file=<.env> spike/with-env.mjs <command> [args...]');
  process.exit(2);
}
// Windows starts .bat/.cmd files only through cmd.exe, which needs a quoted path with backslashes
const viaShell = /\.(bat|cmd)$/i.test(command);
const executable = viaShell ? `"${resolve(command)}"` : command;
const result = spawnSync(executable, args, { stdio: 'inherit', env: process.env, shell: viaShell });
process.exit(result.status ?? 1);
