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
.PHONY: default init clean clean-all dist sync-main-repo sanatize update-json
.PHONY: init-1.12 clean-1.12 clean-all-1.12 dist-1.12 sanatize-1.12
.PHONY: init-1.13 clean-1.13 clean-all-1.13 dist-1.13 sanatize-1.13

default:	;	@echo "(You are not in a MC specific version directory)"
clean: clean-1.12 clean-1.13
clean-all: clean-all-1.13
init: init-1.12 init-1.13

clean-1.12: 		; -@cd 1.12; make -s clean
clean-1.13: 		; -@cd 1.13; make -s clean
clean-all-1.12: ;	-@cd 1.12; make -s clean-all
clean-all-1.13: ;	-@cd 1.13; make -s clean-all
init-1.12:			; -@cd 1.12; make -s init
init-1.13:			; -@cd 1.13; make -s init
dist-1.12:			; @cd 1.12; make -s dist
dist-1.13:			; @cd 1.13; make -s dist
dist: dist-1.12 dist-1.13 | update-json

update-json:
	@echo "[main] Update update.json ..."
	@djs tasks.js update-json

sanatize:
	@cd 1.12; make -s sanatize
	@cd 1.13; make -s sanatize
	@make -s update-json

# For reviewers: I am using a local repository for experimental changes,
# this target copies the local working tree to the location of the
# repository that you cloned.
sync-main-repo: sanatize update-json
	@echo "[main] Synchronising to github repository working tree ..."
	@djs tasks.js sync-main-repository
