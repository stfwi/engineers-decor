"use strict";
(function(constants){
  var me = {};

  /**
   * Loads the raw data of the lang file.
   * @returns {object}
   */
  me.load_raw = function(file_path, remove_comments) {
    const lang_code = fs.basename(file_path).replace(/\..*$/,"").trim().toLowerCase();
    var lines = fs.readfile(file_path).trim().split("\n");
    var was_eol_escape = false;
    for(var i in lines) {
      if(was_eol_escape) {
        var k=0;
        for(k=i-1; k>=0; --k) {
          if(lines[k] != null) {
            lines[k] += "\n" + lines[i];
            break;
          }
        }
        was_eol_escape = lines[i].match(/[^\\][\\]$/) != null;
        lines[i] = null;
      } else {
        lines[i] = lines[i].trim();
        was_eol_escape = lines[i].match(/[^\\][\\]$/) != null;
      }
    }
    lines = lines.filter(function(l){return (l!==null)});
    if(!!remove_comments) lines = lines.filter(function(l){return (l.trim().search(/^#/)!=0)});
    return { code:lang_code, data:lines };
  }

  /**
   * Loads a language into a unified object format
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
    var lines = me.load_raw(file_path, true).data.map(function(line){return line.replace(/[\s]*[\\][\r]?[\n][\s]*/mig, " ").trim()});
    for(var i in lines) {
      if(!lines[i].length) continue;
      const kv = lines[i].split("=", 2);
      if(kv.length!=2) throw new Error("Invalid line " + i + " in '"+file_path+"': '"+lines[i]+"'");
      const key = kv[0].trim();
      const text = kv[1].trim();
      text = text.replace("\\\\n", "\n").replace("\\n", "\n");
      if(key.length == 0) {
        throw new Error("Empty key in '"+file_path+"' line '" + lines[i] + "'");
      } else if(key.search("tile."+modid+".")==0) {
        key = key.replace("tile."+modid+".", "");
        key = key.split(".", 2);
        const block = key[0];
        const prop = key[1];
        if(data.blocks[block]===undefined) data.blocks[block] = {};
        data.blocks[block][prop] = text;
      } else if(key.search("item."+modid+".")==0) {
        key = key.replace("item."+modid+".", "");
        key = key.split(".", 2);
        const item = key[0];
        const prop = key[1];
        if(data.items[item]===undefined) data.blocks[item] = {};
        data.blocks[item][prop] = text;
      } else if(key.search(modid + ".config.title")==0) {
        data.config_title = text;
      } else if(key.search("itemGroup.tab" + modid)==0) {
        data.creative_tab = text;
      } else if(key.search(modid + ".")==0) {
        data.other[key] = text;
      } else {
        data.invalid[key] = text;
      }
    }
    const lang_code = fs.basename(file_path).replace(/[\.].*$/, "").trim().toLowerCase();
    if(constants.languages[lang_code] === undefined) throw new Error("No language header constants defined for '" + lang_code + "'");
    data.lang = constants.languages[lang_code];
    return data;
  }

  /**
   * Saves a language in the version specific MC format from
   * a unified object format.
   */
  me.save = function(file_path, lang_data) {
    throw new Error("lang.save() not implemented yet for 1.12 lang files.");
  }

  /**
   * Adds missing entries to the language file, master is en_us.
   * Applies to the CWD and 1.12.2 lang files.
   * @returns {void}
   */
  me.sync_languages = function(reflang_code) {
    if(reflang_code===undefined) reflang_code = "en_us";
    reflang_code = reflang_code.trim().toLowerCase();
    function load() {
      var lang_data = {};
      fs.find("./src/main/resources/assets/"+ constants.mod_registry_name() +"/lang", '*.lang', function(f){
        const r = me.load_raw(f);
        lang_data[r.code] = r.data;
        return false;
      });
      return lang_data;
    }
    function reference_content(lang_data, reflang_code) {
      var lang_lines = [];
      for(var i in lang_data[reflang_code]) {
        var txt = lang_data[reflang_code][i].trim();
        if((txt.search(/^#/)>=0) || (txt.search("=")<0)) { lang_lines.push(txt); continue; }; // comment "#" or empty line in the ref lang file
        var kv = txt.split("=", 2);
        var key = kv[0].trim();
        var val = kv[1].trim();
        var o = {key:key, tr:{}};
        o.tr[reflang_code] = val;
        lang_lines.push(o);
      }
      delete lang_data[reflang_code];
      return lang_lines;
    }
    function add_language(lang_lines, lang_name, lang_data) {
      const find_line = function(lines, key) {
        for(var i in lines) {
          if((typeof(lines[i]) !== "object")) continue;
          if(lines[i].key.toLowerCase()==key.toLowerCase()) return i;
        }
        return -1;
      };
      for(var i in lang_data) {
        var txt = lang_data[i].trim();
        if(txt.search(/^#/)>=0) continue;
        if(txt.search("=")<0) continue;
        var kv = txt.split("=", 2);
        var key = kv[0].trim();
        var val = kv[1].trim();
        var line_i = find_line(lang_lines, key);
        if(line_i >= 0) {
          lang_data[i] = undefined;
          lang_lines[line_i].tr[lang_name] = val;
        }
      }
      return lang_data;
    }

    function complete_lang_lines(lang_lines, lang_names, reflang_code) {
      var lang_outputs = {};
      for(var i in lang_names) lang_outputs[lang_names[i]] = [];
      for(var i_line in lang_lines) {
        var entry = lang_lines[i_line];
        if(typeof(entry) !== "object") {
          for(var i in lang_names) lang_outputs[lang_names[i]].push(entry);
        } else {
          for(var i in lang_names) {
            var name = lang_names[i];
            if(entry.tr[name] !== undefined) {
              lang_outputs[name].push(entry.key + "=" + entry.tr[name]);
            } else {
              var added = entry.key + "=" + entry.tr[reflang_code];
              if((entry.key.search(/\.tip$/)>0) || (entry.key.search(/\.help$/)>0)) added = "#" + added;
              lang_outputs[name].push(added);
              if(added.search(/^#/)<0) print("[warn] Lang: Added default language for missing entry in " + name + ": '" + added + "'");
            }
          }
        }
      }
      return lang_outputs;
    }

    var lang_data = load();
    var lang_names = Object.keys(lang_data);
    var lang_lines = reference_content(lang_data, reflang_code);
    for(var lang_name in lang_data) {
      lang_data[lang_name] = add_language(lang_lines, lang_name, lang_data[lang_name]);
      lang_data[lang_name] = lang_data[lang_name].filter(function(l){ return !!l; });
      if(lang_data[lang_name].length == 0) delete lang_data[lang_name];
    }
    var output_data = complete_lang_lines(lang_lines, lang_names, reflang_code);
    for(var i in output_data) output_data[i] = output_data[i].join("\n") + "\n\n";

    // Remaining lines in lang files (not in the reference lang file)
    for(var lang_name in lang_data) {
      for(var i in lang_data[lang_name]) {
        if(lang_data[lang_name][i].search(/^#/)<0) {
          var added = "# " + lang_data[lang_name][i].replace(/^[#\s]+/,"");
          output_data[lang_name] += added + "\n";
          print("[warn] Lang: Commented out unknown key in " + lang_name + ": '" + added + "'");
        }
      }
    }
    for(var name in output_data) output_data[name] = output_data[name].trim() + "\n";

    for(var name in output_data) {
      fs.writefile("./src/main/resources/assets/"+ constants.mod_registry_name() +"/lang/" + name + ".lang", output_data[name]);
    }
  };

  Object.freeze(me);
  return me;
});