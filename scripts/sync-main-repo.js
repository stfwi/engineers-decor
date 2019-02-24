#!/usr/bin/djs
"use strict";
const main_repo_local = fs.realpath("../engineersdecor-github");
if((!fs.chdir(fs.dirname(fs.realpath(sys.script))+"/..")) || (!fs.isdir(".git"))) throw new Error("Failed to switch to mod source directory.");
const test_repo_local = fs.cwd();
if(main_repo_local == "") throw new Error("Main repository (real) path not found.");
if(fs.realpath(main_repo_local) == fs.realpath(test_repo_local)) throw new Error("This is already the main repository");
if((!fs.chdir(main_repo_local)) || (!fs.isdir(".git"))) throw new Error("Failed to switch to main repository directory.");
if(fs.cwd().search("-github") < 0) throw new Error("Main repository is missing the '*-github' tag in the path name.");
sys.shell("rm -rf build documentation gradle meta scripts src")
sys.shell("rm -f .gitignore build.gradle gradle.properties gradlew gradlew.bat license Makefile readme.md")
if((!fs.chdir(test_repo_local)) || (!fs.isdir(".git"))) throw new Error("Failed to switch to local dev directory.");
sys.shell("cp -r documentation gradle meta scripts src " + main_repo_local + "/")
sys.shell("cp .gitignore build.gradle gradle.properties gradlew gradlew.bat license Makefile readme.md " + main_repo_local + "/")
if((!fs.chdir(main_repo_local)) || (!fs.isdir(".git"))) throw new Error("Failed to switch to main repository directory.");
print(sys.shell("git status -s"))
