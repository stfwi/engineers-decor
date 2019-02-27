"use strict";
const main_repo_local = fs.realpath("../engineersdecor-github");
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
  if((!fs.chdir(fs.dirname(fs.realpath(sys.script))+"/..")) || (!fs.isdir(".git"))) throw new Error("Failed to switch to mod source directory.");
  const test_repo_local = fs.cwd();
  if(main_repo_local == "") throw new Error("Main repository (real) path not found.");
  if(fs.realpath(main_repo_local) == fs.realpath(test_repo_local)) throw new Error("This is already the main repository");
  if((!fs.chdir(main_repo_local)) || (!fs.isdir(".git"))) throw new Error("Failed to switch to main repository directory.");
  if(fs.cwd().search("-github") < 0) throw new Error("Main repository is missing the '*-github' tag in the path name.");
  // sys.shell("rm -rf build documentation gradle meta scripts src")
  // sys.shell("rm -f .gitignore build.gradle gradle.properties gradlew gradlew.bat license Makefile readme.md")
  // if((!fs.chdir(test_repo_local)) || (!fs.isdir(".git"))) throw new Error("Failed to switch to local dev directory.");
  // sys.shell("cp -r documentation gradle meta scripts src " + main_repo_local + "/")
  // sys.shell("cp .gitignore build.gradle gradle.properties gradlew gradlew.bat license Makefile readme.md " + main_repo_local + "/")
  // if((!fs.chdir(main_repo_local)) || (!fs.isdir(".git"))) throw new Error("Failed to switch to main repository directory.");
  // print(sys.shell("git status -s"))
};

const task_name = sys.args[0];
if((task_name===undefined) || (tasks[task_name])===undefined) {
  alert("No task ", task_name);
  exit(1);
} else {
  tasks[task_name]();
}
