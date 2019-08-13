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
DJS=djs
else
GRADLE=./gradlew --no-daemon
GRADLE_STOP=./gradlew --stop
DJS=djs
endif

wildcardr=$(foreach d,$(wildcard $1*),$(call wildcardr,$d/,$2) $(filter $(subst *,%,$2),$d))

#
# Targets
#
.PHONY: default mod init clean clean-all mrproper all run install sanatize dist-check dist start-server assets

default: mod

all: clean clean-all mod | install

mod:
	@echo "[1.14] Building mod using gradle ..."
	@$(GRADLE) build $(GRADLE_OPTS)

clean:
	@echo "[1.14] Cleaning ..."
	@rm -rf mcmodsrepo
	@rm -f build/libs/*
	@$(GRADLE) clean

clean-all:
	@echo "[1.14] Cleaning using gradle ..."
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
	@echo "[1.14] Initialising eclipse workspace using gradle ..."
	@$(GRADLE) eclipse

sanatize:
	@echo "[1.14] Running sanatising tasks ..."
	@djs tasks.js trailing-whitespaces
	@djs tasks.js tabs-to-spaces
	@djs tasks.js sync-languages
	@djs tasks.js version-check
	@djs tasks.js update-json
	@git status -s .

dist-check:
	@echo "[1.14] Running dist checks ..."
	@djs tasks.js dist-check

dist: sanatize dist-check clean-all mod
	@echo "[1.14] Distribution files ..."
	@mkdir -p dist
	@cp build/libs/$(MOD_JAR_PREFIX)* dist/
	@djs tasks.js dist

assets:
	@echo "[1.14] Running asset generators ..."
	@djs tasks.js create-slab-assets
	@djs tasks.js create-half-slab-assets
	@djs tasks.js assets
