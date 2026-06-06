# Deploy Vercel prebuilt artifacts from GitHub CI

Vercel's build image has no JVM, so building the cljs SPA there would mean installing Java + Clojure on every push — duplicating the toolchain CI already provisions. Instead, CI runs `vercel build` / `vercel deploy --prebuilt`, and Vercel's own Git auto-deploys are turned off via `vercel.json` (`git.deploymentEnabled: false`) so failed Vercel builds don't show up as red checks on PRs.
