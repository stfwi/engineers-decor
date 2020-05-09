"use strict";
(function(constants){
  var me = {};

  const clone = function(o) { return JSON.parse(JSON.stringify(o)); }

  /**
   * Loads the raw data of the lang file.
   * @returns {object}
   */
  me.load_raw = function(file_path) {
    const lang_code = fs.basename(file_path).replace(/\..*$/,"").trim().toLowerCase();
    const data = JSON.parse(fs.readfile(file_path).trim());
    return { code:lang_code, data:data };
  }

  /**
   * Loads
   */
  me.load = function(file_path) {
    const modid = constants.mod_registry_name();
    const data = {
      creative_tab: "",
      config_title: "",
      blocks: {},
      items: {},
      other: {},
      lang: {},
      invalid: {}
    };
    var lines = me.load_raw(file_path).data;
    for(var objkey in lines) {
      const key = objkey.trim();
      const text = lines[objkey].trim();
      if(key.length == 0) {
        throw new Error("Empty key in '"+file_path+"' line '" + lines[i] + "'");
      } else if(key.search("block."+modid+".")==0) {
        key = key.replace("block."+modid+".", "");
        key = key.split(".", 2);
        const block = key[0];
        const prop = ((key.length<2) || (key[1]=="")) ? "name" : key[1];
        if(data.blocks[block]===undefined) data.blocks[block] = {};
        data.blocks[block][prop] = text;
      } else if(key.search("item."+modid+".")==0) {
        key = key.replace("item."+modid+".", "");
        key = key.split(".", 2);
        const item = key[0];
        const prop = ((key.length<2) || (key[1]=="")) ? "name" : key[1];
        if(data.items[item]===undefined) data.blocks[item] = {};
        data.blocks[item][prop] = text;
      } else if(key.search(modid + ".config.title")==0) {
        data.config_title = text;
      } else if(key.search("itemGroup.tab" + modid)==0) {
        data.creative_tab = text;
      } else if(key.search(modid + ".")==0) {
        data.other[key] = text;
      } else if(key.search("language")==0) {
        key = key.replace("language", "");
        key = key.split(".", 2);
        const prop = ((key.length<2) || (key[1]=="")) ? "name" : key[1];
        data.lang[prop] = text;
      } else {
        data.invalid[key] = text;
      }
    }
    return data;
  }

  /**
   * Saves a language in the version specific MC format from
   * a unified object format.
   */
  me.save = function(file_path, data) {
    if(Object.keys(data.invalid).length > 0) throw new Error("Given language data have entries in the marked-invalid data, fix this first.");
    const modid = constants.mod_registry_name();
    var out = {};
    out["language"] = data.lang.name;
    out["language.code"] = data.lang.code;
    out["language.region"] = data.lang.region;
    out["itemGroup.tab" + modid] = data.creative_tab;
    out[modid+".config.title"] = data.config_title;
    for(var it in data.other) {
      out[it] = data.other[it];
    }
    for(var blkname in data.blocks) {
      var blk = data.blocks[blkname];
      for(var key in blk) {
        if(key=="name") {
          out["block."+modid+"."+blkname] = blk[key];
        } else {
          out["block."+modid+"."+blkname+"."+key] = blk[key];
        }
      }
    }
    for(var itemname in data.items) {
      var item = data.items[itemname];
      for(var key in item) {
        if(key=="name") {
          out["item."+modid+"."+itemname] = item[key];
        } else {
          out["item."+modid+"."+itemname+"."+key] = item[key];
        }
      }
    }
    var txt = JSON.stringify(out,null,1);
    var file_lang_code = fs.basename(file_path).replace(/\.json/,"");
    const filename = fs.basename(file_path);
    if(filename.toLowerCase() != filename) throw new Error("Language files must be completely lowercase.");
    if(file_lang_code != data.lang.code) throw new Error("File name to save does not contain the language code of the given data.");
    if(filename.search("\.json$") <= 0) throw new Error("File name to save must be a json file (lowercase).");
    if(!fs.isdir(fs.dirname(file_path))) throw new Error("File to save: Parent directory does not exist.");
    fs.writefile(file_path, txt);
  }

  /**
   * Adds missing entries to the language file, master is en_us.
   * Applies to the CWD and 1.12.2 lang files.
   * @returns {void}
   */
  me.sync_languages = function(reflang_code) {
    const modid = constants.mod_registry_name();
    if(reflang_code===undefined) reflang_code = "en_us";
    reflang_code = reflang_code.trim().toLowerCase();
    function load() {
      const lang_data = {};
      fs.find("./src/main/resources/assets/"+ modid +"/lang", '*.json', function(f){
        const r = me.load_raw(f);
        lang_data[r.code] = r.data;
        return false;
      });
      return lang_data;
    }
    function sync_lang_data(lang_data, reflang_code) {
      const lang_outputs = clone(lang_data);
      const reflang = lang_data[reflang_code];
      for(var name in lang_data) {
        if(name == reflang_code) continue;
        const lang = lang_outputs[name];
        for(var key in reflang) {
          if(lang[key] === undefined) {
            lang[key] = clone(reflang[key]);
            print("[warn] Lang: Added default language for missing entry in " + name + ": '" + key + "'");
          }
        }
        for(var key in lang) {
          if((reflang[key] === undefined) && (lang[key].search(/^_/)<0)) {
            lang["_"+key] = lang[key];
            print("[warn] Lang: Commented out obsolete entry in " + name + ": '" + key + "'");
          }
        }
      }
      return lang_outputs;
    }

    const sort_entries = function(data) {
      data = clone(data);
      const out = {};
      const move = function(key) { out[key] = data[key]; data[key] = undefined; delete data[key]; };
      move("language");
      move("language.code");
      move("language.region");
      move("itemGroup.tab"+modid);
      move(modid+".config.title");
      const keys = Object.keys(data);
      keys.sort(function(a, b) {
        if((a.search(modid+".config.")==0) && (b.search(modid+".config.")<0) ) return -1;
        if((b.search(modid+".config.")==0) && (a.search(modid+".config.")<0) ) return +1;
        if((a.search(modid+".tooltip.")==0) && (b.search(modid+".tooltip.")<0) ) return -1;
        if((b.search(modid+".tooltip.")==0) && (a.search(modid+".tooltip.")<0) ) return +1;
        if((a.search("block."+modid+".")==0) && (b.search("block."+modid+".")<0) ) return -1;
        if((b.search("block."+modid+".")==0) && (a.search("block."+modid+".")<0) ) return +1;
        if((a.search("item."+modid+".")==0) && (b.search("item."+modid+".")<0) ) return -1;
        if((b.search("item."+modid+".")==0) && (a.search("item."+modid+".")<0) ) return +1;
        return (a>b ? 1 : (a<b ? -1 : 0));
      });
      for(var i in keys) move(keys[i]);
      return out;
    };

    const output_data = sync_lang_data(load(), reflang_code);
    for(var name in output_data) {
      var data = sort_entries(output_data[name]);
      data = JSON.stringify(data, null, 2);
      fs.writefile("./src/main/resources/assets/" + modid + "/lang/" + name + ".json", data);
      // print("--------------------------------------------------------------------------------");
      // print(output_data[name]);
    }
  };

  Object.freeze(me);
  return me;
});
