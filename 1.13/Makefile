# @file Makefile
# @author Stefan Wilhelm (wile)
# @license MIT
#
# GNU Make makefile for, well, speeding
# up the development a bit.
# You very likely need some tools installed
# to use all build targets, so this file is
# not "official". If you work on windows and
# install GIT with complete shell PATH (the
# red marked option in the GIT installer) you
# should have the needed unix tools available.
# For image stripping install imagemagick and
# also put the "magick" executable in the PATH.
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
.PHONY: default mod init clean clean-all all run install sanatize dist-check dist start-server

default: mod

all: clean clean-all mod | install

mod:
	@echo "[1.13] Building mod using gradle ..."
	@$(GRADLE) build $(GRADLE_OPTS)

clean:
	@echo "[1.13] Cleaning ..."
	@rm -f build/libs/*
	@$(GRADLE) clean

clean-all:
	@echo "[1.13] Cleaning using gradle ..."
	@rm -f dist/*
	@$(GRADLE) clean cleanCache

init:
	@echo "[1.13] Initialising eclipse workspace using gradle ..."
	@$(GRADLE) eclipse

sanatize:
	@echo "[1.13] Running sanatising tasks ..."
	@djs tasks.js trailing-whitespaces
	@djs tasks.js tabs-to-spaces
	@djs tasks.js sync-languages
	@djs tasks.js version-check
	@djs tasks.js update-json
	@git status -s .

dist-check:
	@echo "[1.13] Running dist checks ..."
	@djs tasks.js dist-check

dist: sanatize dist-check clean-all mod
	@echo "[1.13] Distribution files ..."
	@mkdir -p dist
	@cp build/libs/$(MOD_JAR_PREFIX)* dist/
	@djs tasks.js dist
