#!/usr/bin/djs
"use strict";
// Note for reviewers/clones: This file is a auxiliary script for my setup. It's not needed to build the mod.
const config_main_repo = fs.realpath("../engineersdecor-github");
if(!fs.chdir(fs.dirname(fs.realpath(sys.script)))) throw new Error("Failed to switch to mod repository base directory.");
if(!fs.isdir(".git")) throw new Error("Missing git repository in mod repository base directory.");
var tasks = {};

tasks["update-json"] = function() {
  const root_dir = fs.realpath(fs.dirname(sys.script));
  var update_jsons = {
    "1.12.2": JSON.parse(fs.readfile(root_dir + "/1.12/meta/update.json")),
    "1.13.2": JSON.parse(fs.readfile(root_dir + "/1.13/meta/update.json"))
  };
  var update_json = {
    homepage: "https://www.curseforge.com/minecraft/mc-mods/engineers-decor/",
    "1.12.2": update_jsons["1.12.2"]["1.12.2"],
    "1.13.2": update_jsons["1.13.2"]["1.13.2"],
    promos: {
      "1.12.2-recommended": update_jsons["1.12.2"]["promos"]["1.12.2-recommended"],
      "1.12.2-latest": update_jsons["1.12.2"]["promos"]["1.12.2-latest"],
      "1.13.2-recommended": update_jsons["1.13.2"]["promos"]["1.13.2-recommended"],
      "1.13.2-latest": update_jsons["1.13.2"]["promos"]["1.13.2-latest"],
    }
  }
  fs.mkdir(root_dir + "/meta");
  fs.writefile(root_dir + "/meta/update.json", JSON.stringify(update_json, null, 2));
};

tasks["sync-main-repository"] = function() {
  // step-by-step-verbose operations, as the code bases and copy data are different.
  if((!fs.chdir(fs.dirname(fs.realpath(sys.script)))) || (!fs.isdir(".git"))) throw new Error("Failed to switch to mod source directory.");
  if(sys.shell("git remote -v") != "") throw new Error("Dev repository has a remote set.");
  if(main_repo_local == "") throw new Error("Main repository (real) path not found.");
  const test_repo_local = fs.cwd();
  const main_repo_local = fs.realpath(config_main_repo);
  if(main_repo_local == fs.realpath(test_repo_local)) throw new Error("This is already the main repository");
  const cd_dev = function(subdir) {
    if((!fs.chdir(test_repo_local)) || (!fs.isdir(".git"))) throw new Error("Failed to switch to mod source directory.");
    if((subdir!==undefined) && (!fs.chdir(subdir))) throw new Error("Failed to change to '" + subdir + "' of the test repository.");
  }
  const cd_main = function(subdir) {
    if((!fs.chdir(main_repo_local)) || (!fs.isdir(".git"))) throw new Error("Failed to switch to main repository directory.");
    if(fs.cwd().search("-github") < 0) throw new Error("Main repository is missing the '*-github' tag in the path name.");
    if((subdir!==undefined) && (!fs.chdir(subdir))) throw new Error("Failed to change to '" + subdir + "' of the main repository.");
  };
  cd_main();
  sys.shell("rm -rf documentation meta");
  sys.shell("rm -f .gitignore credits.md license Makefile readme.md tasks.js");
  cd_main("1.12"); sys.shell("rm -rf meta src gradle");
  cd_main("1.13"); sys.shell("rm -rf meta src gradle");
  cd_dev();
  sys.shell("cp -f .gitignore credits.md license Makefile readme.md tasks.js \"" + main_repo_local + "/\"")
  sys.shell("cp -r documentation meta \"" + main_repo_local + "/\"")
  cd_dev("1.12");
  sys.shell("cp -f build.gradle gradle.properties gradlew gradlew.bat Makefile readme.md tasks.js signing.* \"" + main_repo_local + "/1.12/\"")
  sys.shell("cp -r src gradle meta \"" + main_repo_local + "/1.12/\"")
  cd_dev("1.13");
  sys.shell("cp -f build.gradle gradle.properties gradlew gradlew.bat Makefile readme.md tasks.js signing.* \"" + main_repo_local + "/1.13/\"")
  sys.shell("cp -r src gradle meta \"" + main_repo_local + "/1.13/\"")
  cd_main();
  print("Main repository changes:");
  print(sys.shell("git status -s"))
};

const task_name = sys.args[0];
if((task_name===undefined) || (tasks[task_name])===undefined) {
  alert("No task ", task_name);
  exit(1);
} else {
  tasks[task_name]();
}
