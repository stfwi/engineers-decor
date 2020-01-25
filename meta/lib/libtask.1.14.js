#!/usr/bin/djs
"use strict";

(function(constants, libassets, liblang){
  const me = {'tasks':{}, 'parsing':{},'sanatizing':{}};

  const note = function() {
    var args = ["[note]"];
    for(var i in arguments) args.push(arguments[i]);
    print.apply(this, args);
  }
  const warn = function() {
    var args = ["[warn]"];
    for(var i in arguments) args.push(arguments[i]);
    print.apply(this, args);
  }
  const pass = function() {
    var args = ["[pass]"];
    for(var i in arguments) args.push(arguments[i]);
    print.apply(this, args);
  }
  const fail = function() {
    var args = ["[fail]"];
    for(var i in arguments) args.push(arguments[i]);
    print.apply(this, args);
  }

  me.tasks.map_regnames_blockstate_filenames = function() {
    const cwd = fs.cwd();
    const rnmap = constants.registryname_map_112_114;
    if(rnmap === undefined) {
      note("Blockstate file renaming skipped, no mapping defined.");
      return;
    }
    try {
      if(!fs.chdir(constants.local_assets_root()+"/blockstates")) throw new Error("Failed to switch to blockstates dir.");
      for(var oldname in rnmap) {
        const oldfile = oldname+".json";
        const newfile = rnmap[oldname]+".json";
        if(oldfile==newfile) continue;
        if(fs.isfile(oldname+".json")) {
          if(fs.isfile(newfile)) {
            note("blockstate file skipped: '" + oldfile + "' -> '" + newfile + "' (new file already exists)");
          } else if(!fs.rename(oldfile, newfile)) {
            note("blockstate file rename failed: '" + oldfile + "' -> '" + newfile + "'");
          } else {
            note("blockstate file: '" + oldfile + "' -> '" + newfile + "'");
          }
        }
      }
      pass("Blockstate file renaming done.");
    } catch(ex) {
      fail("Blockstate file renaming failed:"+ex);
    } finally {
      fs.chdir(cwd);
    }
  };

  me.tasks.map_regnames_lang_file_keys = function() {
    const cwd = fs.cwd();
    const rnmap = constants.registryname_map_112_114;
    const block_prefix = "block." + constants.modid + ".";
    const item_prefix = "item." + constants.modid + ".";
    try {
      if(!fs.chdir(constants.local_assets_root()+"/lang")) throw new Error("Failed to switch to lang dir.");
      const langfiles = fs.readdir();
      for(var i_langfile in langfiles) {
        const original_lang = JSON.parse(fs.readfile(langfiles[i_langfile]));
        const replaced_lang = {}
        for(var key in original_lang) {
          if(key.search(block_prefix)===0) {
            const regname = key.replace(block_prefix,"").replace(/[\.].*$/,"");
            if(rnmap[regname] !== undefined) {
              const sfind = block_prefix + regname;
              const srepl = block_prefix + rnmap[regname];
              const newkey = key.replace(sfind, srepl);
              replaced_lang[newkey] = original_lang[key];
              continue;
            }
          } else if(key.search(item_prefix)===0) {
            const regname = key.replace(item_prefix,"").replace(/[\.].*$/,"");
            if(rnmap[regname] !== undefined) {
              const sfind = item_prefix + regname;
              const srepl = item_prefix + rnmap[regname];
              const newkey = key.replace(sfind, srepl);
              replaced_lang[newkey] = original_lang[key];
              continue;
            }
          }
          // replacements must continue
          replaced_lang[key] = original_lang[key];
        }
        fs.writefile(langfiles[i_langfile], JSON.stringify(replaced_lang,null,1));
      }
      pass("Lang file key mappings done.");
    } catch(ex) {
      warn("Lang file key mapping failed:"+ex);
    } finally {
      fs.chdir(cwd);
    }
  };

  me.tasks.patch_texture_paths_in_models = function() {
    const cwd = fs.cwd();
    const replacements = {};
    replacements['"' + constants.modid+":blocks/"] = '"' + constants.modid+":block/"
    try {
      if(!fs.chdir(constants.local_assets_root()+"/models")) throw new Error("Failed to switch to models dir.");
      fs.find(".", "*.json", function(path){
        const original_text = fs.readfile(path);
        var replaced_text = ""+original_text;
        JSON.parse(replaced_text); // to throw on load error
        for(var sfind in replacements) replaced_text = replaced_text.split(sfind).join(replacements[sfind]);
        if(replaced_text !== original_text) {
          note("Replacements in model '"+path+"'");
          fs.writefile(path, replaced_text);
        }
        return false;
      });
      pass("Model file texture paths done.");
    } catch(ex) {
      fail("Model file texture paths failed:"+ex);
    } finally {
      fs.chdir(cwd);
    }
  };

  me.tasks.create_missing_block_items = function() {
    const cwd = fs.cwd();
    const blockstate_models = {};
    try {
      if(!fs.chdir(constants.local_assets_root())) throw new Error("Failed to switch to assets dir.");
      fs.find("blockstates", "*.json", function(path){
        const blockstate = fs.basename(path).replace(".json","");
        const json = JSON.parse(fs.readfile(path));
        if(json["forge_marker"] !== undefined) {
          var model = json["defaults"]["model"];
          if(model.search(constants.modid+":block/") < 0) {
            model = model.replace(constants.modid+":", constants.modid+":block/");
          }
          blockstate_models[blockstate] = model;
        }
        if(blockstate_models[blockstate] === undefined) {
          throw new Error("IMPLEMENT use first found model.");
        }
        return false;
      });
      for(var blockstate in blockstate_models) {
        const item_model_file = "models/item/"+blockstate+".json";
        if(fs.isfile(item_model_file)) continue;
        if(!fs.writefile(item_model_file, JSON.stringify({parent:blockstate_models[blockstate]}))) {
          throw new Error("Failed to write item model file '" + item_model_file + "'");
        }
      }
      pass("Missing item models done.");
    } catch(ex) {
      fail("Missing item models failed:"+ex);
    } finally {
      fs.chdir(cwd);
    }
  };

  me.tasks.patch_registry_names_in_java_files = function() {
    const cwd = fs.cwd();
    const rnmap = constants.registryname_map_112_114;
    if(rnmap === undefined) {
      pass("Registry name mapping in java files skipped, no mappings defined.");
      return;
    }
    try {
      if(!fs.chdir("src/main/java")) throw new Error("Failed to switch to 'src/main/java'.");
      fs.find(".", "*.java", function(path){
        const original_code = fs.readfile(path);
        var replaced_code = ""+original_code;
        if(original_code===undefined) throw new Error("Failed to read '"+ path +"'.");
        for(var oldname in rnmap) {
          if(oldname == rnmap[oldname]) {
            continue;
          }
          {
            const sfind = '"'+constants.modid+':'+oldname+'"';
            const srepl = '"'+constants.modid+':'+rnmap[oldname]+'"';
            if(replaced_code.search(sfind) >= 0) {
              replaced_code = replaced_code.split(sfind).join(srepl);
              note(fs.basename(path), ":", sfind, "->" , srepl);
            }
          }
          {
            const sfind = '"'+oldname+'"';
            const srepl = '"'+rnmap[oldname]+'"';
            if(replaced_code.search(sfind) >= 0) {
              replaced_code = replaced_code.split(sfind).join(srepl);
              note(fs.basename(path), ":", sfind, "->" , srepl);
            }
          }
        }
        if(replaced_code !== original_code) {
          if(!fs.writefile(path, replaced_code)) throw new Error("Failed to write '"+ path +"'.");
        }
        return false;
      });
      pass("Registry name mapping in java files patched.");
    } catch(ex) {
      fail("Registry name mapping in java files failed:"+ex);
    } finally {
      fs.chdir(cwd);
    }
  };

  me.tasks.patch_forge_blockstates = function() {
    const cwd = fs.cwd();
    try {
      if(!fs.chdir(constants.local_assets_root()+"/blockstates")) throw new Error("Failed to switch to blockstates dir.");
      const blockstate_files = fs.readdir(".");
      for(var fit in blockstate_files) {
        const jtxt = fs.readfile(blockstate_files[fit]);
        if(!jtxt) throw new Error("Failed read blockstate file '" + blockstate_files[fit] + "'.");
        const json = JSON.parse(jtxt);
        if(json["forge_marker"] !== 1) continue;
        // now simply text replace to keep the formatting
        var njtext = jtxt.replace(/"normal"[\s]*:/, '"":');
        njtext = njtext.replace(/"inventory"[\s]*:[\s]*\[[\s]*\{[\s]*\}[\s]*][\s]*[,]?[\s]*/, '');
        njtext = njtext.replace(/"model":[\s]*"/g, '"model": "');
        const pref = '"model": "' + constants.modid + ':';
        njtext = njtext.replace(new RegExp(pref, "g"), pref + 'block/'); // actually faster to simply replace all and correct doubles.
        njtext = njtext.replace(new RegExp(pref + 'block/block/', "g"), pref + 'block/');
        njtext = njtext.replace("\n\n", "\n");
        if(jtxt !== njtext) {
          fs.writefile(blockstate_files[fit], njtext);
          note("Forge blockstate '"+ fs.basename(blockstate_files[fit]) +"' patched.");
        }
      }
      pass("Forge blockstates patched.");
    } catch(ex) {
      fail("Forge blockstate patching failed:"+ex);
    } finally {
      fs.chdir(cwd);
    }
  };

  me.tasks.map_recipe_filenames = function() {
    const cwd = fs.cwd();
    const rnmap = constants.registryname_map_112_114;
    if(rnmap === undefined) {
      pass("Recipe file name mapping skipped, no mappings defined.");
      return;
    }
    try {
      if(!fs.chdir(constants.local_data_root()+"/recipes")) throw new Error("Failed to switch to recipes dir.");
      fs.find(".", "*.json", function(path){
        const file_name = fs.basename(path);
        if(file_name.search("_")===0) return;
        const oldfile = path;
        var newfile = "";
        for(var oldname in rnmap) {
          const newname = rnmap[oldname];
          if(file_name.search(oldname)===0) {
            newfile = fs.dirname(path) + "/" + fs.basename(path).replace(oldname, newname);
            newfile = newfile.replace(".json","");
            if((newfile.search(/_recipe$/)<0) && (newfile.search(/_backcycle$/)<0) && (newfile.search(/_standalone$/)<0)) {
              newfile += "_recipe";
            }
            newfile += ".json";
            break;
          }
        }
        if(newfile == "") {
          // no match
        } else if(oldfile === newfile) {
          note("skip identical file " + newfile);
        } else {
          note(oldfile + " -> " + newfile);
          fs.rename(oldfile, newfile);
        }
      });
      pass("Recipe file name mapping done.");
    } catch(ex) {
      fail("Recipe file name mapping failed:"+ex);
    } finally {
      fs.chdir(cwd);
    }
  };

  me.tasks.map_recipe_contents = function() {
    const modid = constants.modid;
    const cwd = fs.cwd();
    const rnmap = constants.registryname_map_112_114;
    if(rnmap === undefined) {
      pass("Recipe json data mapping skipped, no mappings defined.");
      return;
    }
    try {
      if(!fs.chdir(constants.local_data_root()+"/recipes")) throw new Error("Failed to switch to recipes dir.");
      fs.find(".", "*.json", function(path){
        if(fs.basename(path).search("_")===0) return;
        const txt = fs.readfile(path);
        if(txt === undefined) {
          note("Failed to read file '" + path + "'");
          return;
        }
        // The easy stuff fist, text replace regnames
        for(var key in rnmap) {
          const oldname = modid+":"+key
          const newname = modid+":"+rnmap[key];
          txt = txt.split('"'+oldname+'"').join('"'+newname+'"');
        }
        txt = JSON.stringify(JSON.parse(txt));
        txt = txt.replace(/,"data":0/g, "");
        var recipe = JSON.parse(txt);
        if(recipe.conditions === undefined) recipe.conditions = {};
        recipe.conditions.type = modid+":grc"
        recipe.conditions.result = recipe.result;
        fs.writefile(path, JSON.stringify(recipe,null,1));
        if((recipe.result===undefined) || (recipe.result.item===undefined)) {
          warn("Recipe '" + path + "': No result item?!");
          return;
        }
        const filename_check = recipe.result.item.replace(/^.*?:/,"");
        if(fs.basename(path).search(filename_check) < 0) {
          warn("Recipe filename '" + path + "' does not contain the result '"+ filename_check +"'.");
          //const newfile = fs.dirname(path) + "/" + filename_check + "_recipe.json";
          //if(!fs.isfile(newfile)) fs.rename(path, newfile);
        }
      });
      pass("Recipe json data mappings done.");
    } catch(ex) {
      fail("Recipe json data mappings failed:"+ex);
    } finally {
      fs.chdir(cwd);
    }
  };

  me.tasks.lang_json_text_replacements = function() {
    var file_list = (function() {
      var ls = [];
      const dir = "./" + constants.local_assets_root() + "/lang";
      if(fs.isdir(dir)) {
        ls = ls.concat(fs.find(dir, '*.json'));
        for(var i in ls) ls[i] = ls[i].replace(/\\/g,"/");
      }
      ls.sort();
      return ls;
    })();

    for(var file_i in file_list) {
      var file = file_list[file_i];
      var txt = fs.readfile(file);
      if(txt===undefined) throw new Error("Failed to read '" + file + "'");
      txt = txt.replace(/\\\\n/g,"\\n");
      fs.writefile(file, txt);
    }
  };


  me.stdtasks = {};
  me.stdtasks["assets"] = function() {
    me.tasks.map_regnames_blockstate_filenames();
    me.tasks.patch_texture_paths_in_models();
    me.tasks.create_missing_block_items();
    me.tasks.patch_registry_names_in_java_files();
    me.tasks.patch_forge_blockstates();
    me.tasks.map_recipe_filenames();
    me.tasks.map_recipe_contents();
    me.tasks.map_regnames_lang_file_keys();
  };

  me.stdtasks["datagen"] = function() {
    sys.exec("gradlew.bat", ["--no-daemon", "runData"]);
    // double check and really only copy json files.
    const dst = fs.realpath("src/main/resources/data/" + constants.modid);
    const src = fs.realpath("src/generated/resources/data/" + constants.modid);
    if(!dst || !src) throw "Source or destination directory not found.";
    const src_files = fs.find(src, "*.json");
    const upath = function(s) { return s.replace(/[\\]/g,"/").replace(/^[\/]/,""); } // for correct display on win32
    if(src_files===undefined) return 1;
    for(var i in src_files) {
      const srcfile = src_files[i];
      const dstfile = srcfile.replace(src, dst);
      const dstdir = fs.dirname(dstfile);
      if(!fs.isdir(dstdir)) fs.mkdir(dstdir);
      if(!fs.isfile(dstfile)) {
        print("[copy] ", upath(srcfile.replace(src,"")));
        fs.copy(srcfile, dstdir);
      } else if(sys.hash.sha1(srcfile,true) != sys.hash.sha1(dstfile,true)) {
        print("[edit] ", upath(srcfile.replace(src,"")));
        fs.unlink(dstfile);
        fs.copy(srcfile, dstdir);
      }
    }
  };

  me.stdtasks["lang-json-fixes"] = function() {
    me.tasks.lang_json_text_replacements();
  }

  Object.freeze(me);
  Object.freeze(me.tasks);
  Object.freeze(me.parsing);
  Object.freeze(me.sanatizing);
  return me;
});
