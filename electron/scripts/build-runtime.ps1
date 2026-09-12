#!/usr/bin/env pwsh
<#
    生成数据层专用的精简 Java 运行时（jlink），不含 JavaFX 模块。
    产物用于随安装包一起分发，客户端无需预装 Java。

        pwsh scripts/build-runtime.ps1
#>
param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$Output = (Join-Path $PSScriptRoot "..\build\runtime")
)

$ErrorActionPreference = "Stop"

if (-not $JdkHome) {
    throw "未找到 JDK：请设置 JAVA_HOME，或通过 -JdkHome 指定"
}

$jlink = Join-Path $JdkHome "bin/jlink.exe"

if (-not (Test-Path $jlink)) {
    $jlink = Join-Path $JdkHome "bin/jlink"
}

if (-not (Test-Path $jlink)) {
    throw "未找到 jlink：$jlink"
}

# 数据层所需模块：JDBC、连接池、日志、SSL、达梦驱动等
$modules = @(
    "java.base",
    "java.sql",
    "java.naming",
    "java.desktop",
    "java.logging",
    "java.management",
    "java.security.sasl",
    "java.transaction.xa",
    "java.xml",
    "java.net.http",
    "java.scripting",
    "jdk.crypto.ec",
    "jdk.unsupported",
    "jdk.zipfs"
) -join ","

if (Test-Path $Output) {
    Remove-Item -LiteralPath $Output -Recurse -Force
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $Output) | Out-Null

& $jlink `
    --add-modules $modules `
    --strip-debug `
    --no-header-files `
    --no-man-pages `
    --compress=zip-6 `
    --output $Output

if ($LASTEXITCODE -ne 0) {
    throw "jlink 执行失败，退出码 $LASTEXITCODE"
}

$size = (Get-ChildItem $Output -Recurse -File | Measure-Object Length -Sum).Sum / 1MB

Write-Host ("运行时已生成: {0} ({1:N1} MB)" -f $Output, $size)
