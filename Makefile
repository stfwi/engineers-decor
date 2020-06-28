# @file Makefile
# @author Stefan Wilhelm (wile)
# @license MIT
#
# GNU Make makefile based build relay.
# Note for reviewers/clones: This file is a auxiliary script for my setup.
# It's not needed to build the mod.
#
.PHONY: default init clean clean-all mrproper sanitize dist dist-all combined-update-json sync-main-repo

default:	;	@echo "First change to specific version directory."
dist: default

clean:
	-@cd 1.12; make -s clean
	-@cd 1.14; make -s clean
	-@cd 1.15; make -s clean

clean-all:
	-@cd 1.12; make -s clean-all
	-@cd 1.14; make -s clean-all
	-@cd 1.15; make -s clean-all

mrproper:
	-@cd 1.12; make -s mrproper
	-@cd 1.14; make -s mrproper
	-@cd 1.15; make -s mrproper

combined-update-json:
	@echo "[main] Update update.json ..."
	@djs meta/lib/tasks.js combined-update-json

sanitize:
	@cd 1.12; make -s sanitize
	@cd 1.14; make -s sanitize
	@cd 1.15; make -s sanitize
	@make -s combined-update-json

init:
	-@cd 1.12; make -s init
	-@cd 1.14; make -s init
	-@cd 1.15; make -s init

dist-all: clean-all init
	-@cd 1.12; make -s dist
	-@cd 1.14; make -s dist
	-@cd 1.15; make -s dist

sync-main-repo: sanitize
	@echo "[main] Synchronising to github repository working tree ..."
	@djs meta/lib/tasks.js sync-main-repository
