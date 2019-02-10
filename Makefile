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
INSTALL_DIR=$(realpath ${APPDATA}/.minecraft)
SERVER_INSTALL_DIR=$(realpath ${APPDATA}/minecraft-server-forge-1.12.2-14.23.5.2768)
DJS=djs
else
GRADLE=./gradlew --no-daemon
GRADLE_STOP=./gradlew --stop
INSTALL_DIR=~/.minecraft
SERVER_INSTALL_DIR=~/.minecraft-server-forge-1.12.2-14.23.5.2768
DJS=djs
endif

wildcardr=$(foreach d,$(wildcard $1*),$(call wildcardr,$d/,$2) $(filter $(subst *,%,$2),$d))

#
# Targets
#
.PHONY: default mod init clean clean-all all run install sanatize dist-check dist start-server sync-main-repo

default: mod

all: clean clean-all mod | install

mod:
	@echo "Building mod using gradle ..."
	@$(GRADLE) build $(GRADLE_OPTS)

clean:
	@echo "Cleaning ..."
	@rm -f build/libs/*
	@$(GRADLE) clean

clean-all: clean
	@echo "Cleaning using gradle ..."
	@rm -f dist/*
	@$(GRADLE) clean cleanCache

init:
	@echo "Initialising eclipse workspace using gradle ..."
	@$(GRADLE) setupDecompWorkspace

run:
	@echo "Running client ..."
	@$(GRADLE) runClient

install: $(MOD_JAR) |
	@sleep 2s
	@if [ ! -d "$(INSTALL_DIR)" ]; then echo "Cannot find installation minecraft directory."; false; fi
	@echo "Installing '$(MOD_JAR)' to '$(INSTALL_DIR)/mods' ..."
	@[ -d "$(INSTALL_DIR)/mods" ] || mkdir "$(INSTALL_DIR)/mods"
	@rm -f "$(INSTALL_DIR)/mods/${MOD_JAR_PREFIX}"*.jar
	@cp -f "$(MOD_JAR)" "$(INSTALL_DIR)/mods/"
	@echo "Installing '$(MOD_JAR)' to '$(SERVER_INSTALL_DIR)/mods' ..."
	@rm -f "$(SERVER_INSTALL_DIR)/mods/${MOD_JAR_PREFIX}"*.jar
	@[ -d "$(SERVER_INSTALL_DIR)/mods" ] && cp -f "$(MOD_JAR)" "$(SERVER_INSTALL_DIR)/mods/"

start-server: install
	@echo "Starting local dedicated server ..."
	@cd "$(SERVER_INSTALL_DIR)" && java -jar forge-1.12.2-14.23.5.2768-universal.jar nogui

sanatize:
	@echo "Running sanatising tasks ..."
	@djs scripts/sanatize-trailing-whitespaces.js
	@djs scripts/sanatize-tabs-to-spaces.js
	@djs scripts/sanatize-sync-languages.js
	@djs scripts/sanatize-version-check.js
	@djs scripts/task-update-json.js
	@git status -s

dist-check:
	@echo "Running dist checks ..."
	@djs scripts/sanatize-dist-check.js

dist: sanatize dist-check clean-all mod
	@echo "Distribution files ..."
	@mkdir -p dist
	@cp build/libs/$(MOD_JAR_PREFIX)* dist/
	@djs scripts/task-dist.js
