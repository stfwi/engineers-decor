#!/usr/bin/djs
"use strict";
// Note for reviewers/clones: This file is a auxiliary script for my setup. It's not needed to build the mod.
const constants = include("meta/lib/constants.js")();
const libtask = include("meta/lib/libtask.js")(constants);
const libassets = include("meta/lib/libassets.js")(constants);
const modid = constants.mod_registry_name();
const with_113 = false;
var tasks = {};

tasks["update-json"] = function() {
  const root_dir = fs.realpath(fs.dirname(sys.script));
  var update_jsons = {
    "1.12.2": JSON.parse(fs.readfile(root_dir + "/1.12/meta/update.json")),
    "1.13.2": JSON.parse(fs.readfile(root_dir + "/1.13/meta/update.json")),
    "1.14.4": JSON.parse(fs.readfile(root_dir + "/1.14/meta/update.json"))
  };
  var update_json = {
    homepage: "https://www.curseforge.com/minecraft/mc-mods/engineers-decor/",
    "1.12.2": update_jsons["1.12.2"]["1.12.2"],
    "1.13.2": update_jsons["1.13.2"]["1.13.2"],
    "1.14.4": update_jsons["1.14.4"]["1.14.4"],
    promos: {
      "1.12.2-recommended": update_jsons["1.12.2"]["promos"]["1.12.2-recommended"],
      "1.12.2-latest": update_jsons["1.12.2"]["promos"]["1.12.2-latest"],
      "1.13.2-recommended": update_jsons["1.13.2"]["promos"]["1.13.2-recommended"],
      "1.13.2-latest": update_jsons["1.13.2"]["promos"]["1.13.2-latest"],
      "1.14.4-recommended": update_jsons["1.14.4"]["promos"]["1.14.4-recommended"],
      "1.14.4-latest": update_jsons["1.14.4"]["promos"]["1.14.4-latest"]
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
  const main_repo_local = fs.realpath("../engineersdecor-github");
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
  cd_main("1.13"); sys.shell("rm -rf meta gradle");
  cd_main("1.14"); sys.shell("rm -rf meta gradle src");
  cd_dev();
  sys.shell("cp -f .gitignore credits.md license Makefile readme.md tasks.js \"" + main_repo_local + "/\"")
  sys.shell("cp -r documentation meta \"" + main_repo_local + "/\"")
  {
    cd_dev("1.12");
    sys.shell("cp -f .gitignore build.gradle gradle.properties gradlew gradlew.bat Makefile readme.md tasks.js.* \"" + main_repo_local + "/1.12/\"")
    sys.shell("cp -r gradle meta src \"" + main_repo_local + "/1.12/\"")
  }
  {
    cd_dev("1.13");
    sys.shell("cp -f .gitignore build.gradle gradle.properties gradlew gradlew.bat Makefile readme.md tasks.js \"" + main_repo_local + "/1.13/\"")
    sys.shell("cp -r gradle meta \"" + main_repo_local + "/1.13/\"")
  }
  {
    cd_dev("1.14");
    sys.shell("cp -f .gitignore build.gradle gradle.properties gradlew gradlew.bat Makefile readme.md tasks.js \"" + main_repo_local + "/1.14/\"")
    sys.shell("cp -r gradle meta src \"" + main_repo_local + "/1.14/\"")
  }
  cd_main();
  print("Main repository changes:");
  print(sys.shell("git status -s"))
};

tasks["compare-textures"] = function(args) {
  if(args.length==0) args.push("");
  // const verbose = (args.find("-v")) || (args.find("--verbose"));   //// NO ARRAY.FIND???? --> fix in JS engine
  const verbose = (args[0]=="-v") || (args[0]=="-verbose");   //// NO ARRAY.FIND???? --> fix in JS engine
  function compare(va, vb) {
    const cmp = libassets.compare_textures(
      va+"/src/main/resources/assets",
      vb+"/src/main/resources/assets"
    );
    const n_diff = Object.keys(cmp.differ).length;
    const n_match = Object.keys(cmp.match).length;
    const n_onlya = Object.keys(cmp.onlyin_a).length;
    const n_onlyb = Object.keys(cmp.onlyin_b).length;
    if(!verbose) {
      if((n_diff==0) && (n_onlya==0) && (n_onlyb==0)) return true;
      print("[warn] Textures of " + va + "<->" + vb + " differ: " + n_match + " matching, " + n_diff +
                    " different, " + n_onlya + " only in " + va + ", " + n_onlyb + " only in " + vb +
                    ". (--verbose for details)");
      return false;
    } else {
      if((n_diff==0) && (n_onlya==0) && (n_onlyb==0)) {
        print("[pass] Textures of " + va + "<->" + vb + " all match.");
        return true;
      }
      for(var key in cmp.differ) {
        print("[warn] Texture of " + va + "<->" + vb + " differs: '" + key + "'.");
      }
      for(var key in cmp.onlyin_a) {
        print("[warn] Texture only in " + va + ": '" + key + "'.");
      }
      for(var key in cmp.onlyin_b) {
        print("[warn] Texture only in " + vb + ": '" + key + "'.");
      }
      return false;
    }
  }
  var ok = true;
  if(with_113 && !compare("1.12", "1.13")) ok = false;
  if(!compare("1.12", "1.14")) ok = false;
  return ok;
};

tasks["migrate-textures"] = function(args) {
  if(args.length==0) args.push("");
  const verbose = (args[0]=="-v") || (args[0]=="-verbose");
  throw new Error("Migration is WIP");
}

tasks["compare-blockstates"] = function(args) {
  if(args.length==0) args.push("");
  const verbose = (args[0]=="-v") || (args[0]=="-verbose");
  const compare = function(va, vb) {
    const cmp = libassets.compare_blockstates(va+"/src/main/resources/assets/"+modid, vb+"/src/main/resources/assets/"+modid);
    const n_onlya = Object.keys(cmp.onlyin_a).length;
    const n_onlyb = Object.keys(cmp.onlyin_b).length;
    if(!verbose) {
      if((n_onlya==0) && (n_onlyb==0)) return true;
      print("[warn] Block states of " + va + "<->" + vb + " differ: " + n_onlya + " only in " + va + ", " + n_onlyb + " only in " + vb + ". (--verbose for details)");
      return false;
    } else {
      if((n_onlya==0) && (n_onlyb==0)) {
        print("[pass] Block states of " + va + "<->" + vb + " all match.");
        return true;
      }
      for(var key in cmp.onlyin_a) {
        print("[warn] Block states only in " + va + ", not " + vb + ": '" + key + "'.");
      }
      for(var key in cmp.onlyin_b) {
        print("[warn] Block states only in " + vb + ", not " + va + ": '" + key + "'.");
      }
      return false;
    }
  }
  var ok = true;
  if(with_113 && !compare("1.12", "1.13")) ok = false;
  if(!compare("1.12", "1.14")) ok = false;
  return ok;
};

libtask.run(tasks, sys.args, true, ".");
