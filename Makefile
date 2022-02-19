# @file Makefile
# @author Stefan Wilhelm (wile)
# @license MIT
#
# GNU Make makefile based build relay.
# Note for reviewers/clones: This file is a auxiliary script for my setup.
# It's not needed to build the mod.
#
MOD_JAR_PREFIX=engineersdecor-
MOD_JAR=$(filter-out %-sources.jar,$(wildcard build/libs/${MOD_JAR_PREFIX}*.jar))

ifeq ($(OS),Windows_NT)
GRADLE=gradlew.bat --no-daemon
GRADLE_STOP=gradlew.bat --stop
else
GRADLE=./gradlew --no-daemon
GRADLE_STOP=./gradlew --stop
endif
TASK=djs ../../zmeta/lib/tasks.js

wildcardr=$(foreach d,$(wildcard $1*),$(call wildcardr,$d/,$2) $(filter $(subst *,%,$2),$d))

#
# Targets
#
.PHONY: default mod data init clean clean-all mrproper all run install sanitize dist-check dist start-server assets

default: mod

all: clean clean-all mod | install

mod:
	@echo "[1.16] Building mod using gradle ..."
	@$(GRADLE) build $(GRADLE_OPTS)

run:
	@echo "[1.16] Run client ..."
	@$(GRADLE) runClient

assets:
	@echo "[1.16] Running asset generators ..."
	@$(TASK) assets

data:
	@echo "[1.16] Running data generators ..."
	@$(TASK) datagen

clean:
	@echo "[1.16] Cleaning ..."
	@rm -rf src/generated
	@rm -rf mcmodsrepo
	@rm -f build/libs/*
	@$(GRADLE) clean

clean-all:
	@echo "[1.16] Cleaning using gradle ..."
	@rm -rf mcmodsrepo
	@rm -f dist/*
	@rm -rf run/logs/
	@rm -rf run/crash-reports/
	@$(GRADLE) clean

mrproper: clean-all
	@rm -f meta/*.*
	@rm -rf run/
	@rm -rf out/
	@rm -f .project
	@rm -f .classpath

init:
	@echo "[1.16] Initialising eclipse workspace using gradle ..."
	@$(GRADLE) eclipse
	-@$(GRADLE) hideOfficialWarningUntilChanged

sanitize:
	@echo "[1.16] Running sanitising tasks ..."
	@$(TASK) sanitize
	@$(TASK) sync-languages
	@$(TASK) version-check
	@$(TASK) update-json
	@git status -s .

install: $(MOD_JAR) |
	@$(TASK) install

start-server: install
	@$(TASK) start-server

dist-check:
	@echo "[1.16] Running dist checks ..."
	@$(TASK) dist-check

dist-files: clean-all init mod
	@echo "[1.16] Distribution files ..."
	@mkdir -p dist
	@cp build/libs/$(MOD_JAR_PREFIX)* dist/
	@$(TASK) dist

dist: sanitize dist-check dist-files
