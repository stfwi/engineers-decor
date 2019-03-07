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
.PHONY: default mod init clean clean-all all run install sanatize dist-check dist start-server

default: mod

all: clean clean-all mod | install

mod:
	@echo "[1.12] Building mod using gradle ..."
	@$(GRADLE) build $(GRADLE_OPTS)

clean:
	@echo "[1.12] Cleaning ..."
	@rm -f build/libs/*
	@$(GRADLE) clean

clean-all: clean
	@echo "[1.12] Cleaning using gradle ..."
	@rm -f dist/*
	@$(GRADLE) clean cleanCache

init:
	@echo "[1.12] Initialising eclipse workspace using gradle ..."
	@$(GRADLE) setupDecompWorkspace

run:
	@echo "[1.12] Running client ..."
	@$(GRADLE) runClient

install: $(MOD_JAR) |
	@sleep 2s
	@if [ ! -d "$(INSTALL_DIR)" ]; then echo "Cannot find installation minecraft directory."; false; fi
	@echo "[1.12] Installing '$(MOD_JAR)' to '$(INSTALL_DIR)/mods' ..."
	@[ -d "$(INSTALL_DIR)/mods" ] || mkdir "$(INSTALL_DIR)/mods"
	@rm -f "$(INSTALL_DIR)/mods/${MOD_JAR_PREFIX}"*.jar
	@cp -f "$(MOD_JAR)" "$(INSTALL_DIR)/mods/"
	@echo "[1.12] Installing '$(MOD_JAR)' to '$(SERVER_INSTALL_DIR)/mods' ..."
	@rm -f "$(SERVER_INSTALL_DIR)/mods/${MOD_JAR_PREFIX}"*.jar
	@[ -d "$(SERVER_INSTALL_DIR)/mods" ] && cp -f "$(MOD_JAR)" "$(SERVER_INSTALL_DIR)/mods/"

start-server: install
	@echo "[1.12] Starting local dedicated server ..."
	@cd "$(SERVER_INSTALL_DIR)" && java -jar forge-1.12.2-14.23.5.2768-universal.jar nogui

sanatize:
	@echo "[1.12] Running sanatising tasks ..."
	@djs tasks.js trailing-whitespaces
	@djs tasks.js tabs-to-spaces
	@djs tasks.js sync-languages
	@djs tasks.js version-check
	@djs tasks.js update-json
	@git status -s .

dist-check:
	@echo "[1.12] Running dist checks ..."
	@djs tasks.js dist-check

dist: sanatize dist-check clean-all mod
	@echo "[1.12] Distribution files ..."
	@mkdir -p dist
	@cp build/libs/$(MOD_JAR_PREFIX)* dist/
	@djs tasks.js dist
