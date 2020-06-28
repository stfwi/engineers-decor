#!/usr/bin/djs
"use strict";
const root_dir = fs.realpath(fs.dirname(sys.script)+"/../..");
const constants = include(fs.dirname(fs.realpath(sys.script)) + "/constants.js")();
const libtask = include(fs.dirname(fs.realpath(sys.script)) + "/libtask.js")(constants);
const modid = constants.mod_registry_name();
var tasks = {};

tasks["combined-update-json"] = function() {
  const update_json = {
    homepage: constants.project_download_inet_page(),
    promos: {}
  };
  var update_json_src = [];
  fs.find(root_dir + "/1.12/meta/", "update*.json", function(path){ update_json_src.push(JSON.parse(fs.readfile(path))); });
  fs.find(root_dir + "/1.14/meta/", "update*.json", function(path){ update_json_src.push(JSON.parse(fs.readfile(path))); });
  fs.find(root_dir + "/1.15/meta/", "update*.json", function(path){ update_json_src.push(JSON.parse(fs.readfile(path))); });
  for(var i in update_json_src) {
    const version_update_json = update_json_src[i];
    for(var key in version_update_json) {
      if(key=="homepage") {
        continue;
      } else if(key=="promos") {
        for(var prkey in version_update_json.promos) {
          update_json.promos[prkey] = version_update_json.promos[prkey];
        }
      } else {
        update_json[key] = version_update_json[key];
      }
    }
  }
  update_json_src = undefined;
  fs.mkdir(root_dir + "/meta");
  fs.writefile(root_dir + "/meta/update.json", JSON.stringify(update_json, null, 2));
};

tasks["sync-main-repository"] = function() {
  // step-by-step-verbose operations, as the code bases and copy data are different.
  if((!fs.chdir(fs.dirname(fs.realpath(sys.script))+"/../..")) || (!fs.isdir(".git"))) throw new Error("Failed to switch to mod source directory.");
  if(sys.shell("git remote -v") != "") throw new Error("Dev repository has a remote set.");
  if(main_repo_local == "") throw new Error("Main repository (real) path not found.");
  const test_repo_local = fs.cwd();
  const main_repo_local = fs.realpath("../"+ constants.mod_registry_name() + "-github");
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
  cd_main("1.12"); sys.shell("rm -rf meta gradle src");
  cd_main("1.14"); sys.shell("rm -rf meta gradle src");
  cd_main("1.15"); sys.shell("rm -rf meta gradle src");
  cd_dev();
  sys.shell("cp -f .gitignore credits.md license Makefile readme.md tasks.js \"" + main_repo_local + "/\"")
  sys.shell("cp -r documentation meta \"" + main_repo_local + "/\"")
  {
    cd_dev("1.12");
    sys.shell("cp -f .gitignore build.gradle gradle.properties gradlew gradlew.bat Makefile readme.md tasks.js.* \"" + main_repo_local + "/1.12/\"")
    sys.shell("cp -r gradle meta src \"" + main_repo_local + "/1.12/\"")
  }
  {
    cd_dev("1.14");
    sys.shell("cp -f .gitignore build.gradle gradle.properties gradlew gradlew.bat Makefile readme.md tasks.js \"" + main_repo_local + "/1.14/\"")
    sys.shell("cp -r gradle meta src \"" + main_repo_local + "/1.14/\"")
  }
  {
    cd_dev("1.15");
    sys.shell("cp -f .gitignore build.gradle gradle.properties gradlew gradlew.bat Makefile readme.md tasks.js \"" + main_repo_local + "/1.15/\"")
    sys.shell("cp -r gradle meta src \"" + main_repo_local + "/1.15/\"")
  }
  cd_main();
  print("Main repository changes:");
  print(sys.shell("git status -s"))
};

libtask.run(tasks, sys.args);
