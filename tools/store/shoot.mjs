/**
 * Renders every Play Store asset from tools/store/assets.html.
 *
 *   node tools/store/shoot.mjs [outDir]      # default: store/
 *
 * Each asset gets a viewport at its exact required size and a screenshot of
 * that viewport, so the PNG is the size Play asks for with no scaling step in
 * between. Needs the Chromium that Playwright already has.
 */
import { chromium } from "/opt/node22/lib/node_modules/playwright/index.mjs";
import { mkdirSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url));
const page = "file://" + resolve(here, "assets.html");
const out = resolve(process.cwd(), process.argv[2] || "store");
mkdirSync(out, { recursive: true });

const ASSETS = [
  ["icon", "icon-512.png", 512, 512],
  ["feature", "feature-graphic-1024x500.png", 1024, 500],
  ["s1", "screenshot-1-home-screen.png", 1080, 1920],
  ["s2", "screenshot-2-one-tap.png", 1080, 1920],
  ["s3", "screenshot-3-nothing-resets.png", 1080, 1920],
  ["s4", "screenshot-4-colours.png", 1080, 1920],
  ["s5", "screenshot-5-share.png", 1080, 1920],
  ["s6", "screenshot-6-pricing.png", 1080, 1920],
];

const browser = await chromium.launch({ args: ["--no-sandbox"] });
for (const [key, file, w, h] of ASSETS) {
  const tab = await browser.newPage({
    viewport: { width: w, height: h },
    deviceScaleFactor: 1,
    ignoreHTTPSErrors: true,
  });
  const errors = [];
  tab.on("pageerror", e => errors.push(String(e)));
  await tab.goto(`${page}?a=${key}`, { waitUntil: "load" });
  await tab.waitForSelector("body[data-done]", { timeout: 15000 });
  await tab.waitForTimeout(350);
  await tab.screenshot({ path: `${out}/${file}` });
  console.log(`${file}  ${w}x${h}${errors.length ? "  ERRORS " + errors[0] : ""}`);
  await tab.close();
}
await browser.close();
