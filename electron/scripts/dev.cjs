"use strict";

/**
 * 一键启动：依赖 → 数据层 → 渲染层 → 客户端窗口。
 *
 *   npm start
 *
 * 数据层只有在缺失或源码比产物新时才会重新构建，日常改前端界面无需等待 Maven。
 * 加 --force-server 可强制重建数据层。
 */

const { spawn, spawnSync } = require("node:child_process");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");

const electronDir = path.resolve(__dirname, "..");
const repoRoot = path.resolve(electronDir, "..");
const serverJar = path.join(repoRoot, "server", "target", "valkyrie-server.jar");
const rendererEntry = path.join(electronDir, "dist", "renderer", "index.html");
const forceServer = process.argv.includes("--force-server");

const SERVER_SOURCES = ["server", "core", "drivers", "utils"].map(name => path.join(repoRoot, name));

/* ------------------------------ 工具 ------------------------------ */

function step(message) {
  process.stdout.write(`\n[valkyrie] ${message}\n`);
}

function run(command, args, options = {}) {
  const result = spawnSync(command, args, {
    stdio: "inherit",
    cwd: options.cwd || repoRoot,
    shell: process.platform === "win32",
    env: { ...process.env, ...options.env }
  });

  if (result.status !== 0) {
    process.stderr.write(`\n[valkyrie] 命令失败: ${command} ${args.join(" ")}\n`);
    process.exit(result.status ?? 1);
  }
}

function newestSourceTime(directory) {
  let newest = 0;

  const walk = current => {
    let entries;

    try {
      entries = fs.readdirSync(current, { withFileTypes: true });
    } catch {
      return;
    }

    for (const entry of entries) {
      if (entry.name === "target" || entry.name === "node_modules" || entry.name.startsWith("."))
        continue;

      const full = path.join(current, entry.name);

      if (entry.isDirectory()) {
        walk(full);
      } else if (/\.(java|xml)$/.test(entry.name)) {
        const time = fs.statSync(full).mtimeMs;

        if (time > newest)
          newest = time;
      }
    }
  };

  walk(directory);
  return newest;
}

/* ------------------------------ 步骤 ------------------------------ */

function ensureDependencies() {
  if (fs.existsSync(path.join(electronDir, "node_modules", "electron"))) {
    step("依赖已就绪");
    return;
  }

  step("首次运行，安装前端依赖（可能耗时几分钟）…");

  const attempts = [
    { args: ["install", "--no-audit", "--no-fund"], label: "默认配置" },
    { args: ["install", "--no-audit", "--no-fund", `--userconfig=${emptyNpmrc()}`], label: "忽略用户级代理配置" }
  ];

  for (const attempt of attempts) {
    process.stdout.write(`[valkyrie] npm ${attempt.args.join(" ")}（${attempt.label}）\n`);

    const result = spawnSync("npm", attempt.args, {
      stdio: "inherit",
      cwd: electronDir,
      shell: process.platform === "win32"
    });

    if (result.status === 0)
      return;
  }

  process.stderr.write("\n[valkyrie] 依赖安装失败，请检查网络或 npm 配置后重试\n");
  process.exit(1);
}

function emptyNpmrc() {
  const file = path.join(os.tmpdir(), "valkyrie-empty-npmrc");

  if (!fs.existsSync(file))
    fs.writeFileSync(file, "");

  return file;
}

function ensureServer() {
  const jarTime = fs.existsSync(serverJar) ? fs.statSync(serverJar).mtimeMs : 0;
  const sourceTime = Math.max(...SERVER_SOURCES.map(newestSourceTime), fs.statSync(path.join(repoRoot, "pom.xml")).mtimeMs);

  if (!forceServer && jarTime > sourceTime) {
    step("数据层已是最新");
    return;
  }

  step("构建数据层（mvn package）…");
  run("mvn", ["-q", "-DskipTests", "-pl", "server", "-am", "package"]);
}

function buildRenderer() {
  step("构建界面…");
  run("npm", ["run", "build:renderer"], { cwd: electronDir });
}

function startClient() {
  step("启动客户端");

  const electronPath = require("electron");
  const child = spawn(electronPath, ["."], {
    stdio: "inherit",
    cwd: electronDir,
    windowsHide: false,
    env: process.env
  });

  child.on("exit", code => process.exit(code ?? 0));
}

/* ------------------------------ 入口 ------------------------------ */

if (!fs.existsSync(rendererEntry))
  fs.mkdirSync(path.dirname(rendererEntry), { recursive: true });

ensureDependencies();
ensureServer();
buildRenderer();
startClient();
