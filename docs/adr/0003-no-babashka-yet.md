# No babashka yet; npm scripts

Babashka would be the idiomatic Clojure choice for build/deploy task scripting, but `package.json` already exists for npm dependencies and the current tasks (build, watch, test, deploy) are one-line shell each. Trigger to introduce `bb.edn`: when build or deploy logic outgrows simple shell strings.
