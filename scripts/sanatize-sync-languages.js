#!/usr/bin/djs
if((!fs.chdir(fs.dirname(fs.realpath(sys.script))+"/..")) || (!fs.isdir("src"))) throw new Error("Failed to switch to mod source directory.");

function load() {
  var lang_data = {};
  fs.find("./src/main/resources/assets/engineersdecor/lang", '*.lang', function(f){
    var lang_code = fs.basename(f).replace(/\..*$/,"").trim().toLowerCase();
    var lines = fs.readfile(f).trim().split("\n");
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
    lang_data[lang_code] = lines.filter(function(l){return (l!==null);});
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

const reflang_code = "en_us";
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
  fs.writefile("./src/main/resources/assets/engineersdecor/lang/" + name + ".lang", output_data[name]);
}
