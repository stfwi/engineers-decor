#!/usr/bin/djs
"use strict";
if(!fs.chdir(fs.dirname(fs.realpath(sys.script)))) throw new Error("Failed to switch to mod source directory.");
if(!fs.isdir("../.git")) throw new Error("Missing git repository in parent directory of mod source.");

var tasks = {};

tasks["dist-check"] = function() {
  const uncommitted_changes = sys.shell("git status -s").trim();
  const gittags = sys.shell('git log -1 --format="%D"')
                  .replace(/[\s]/g,"").split(",")
                  .filter(function(s){ return s.indexOf("tag:")==0;})
                  .map(function(s){ return s.replace(/^tag:/,"");});
  const version_engineersdecor = fs.readfile("gradle.properties", function(line){
    if(line.trim().indexOf("version_engineersdecor")!=0) return false;
    return line.replace(/^.*?=/,"").trim()
  }).trim();
  const git_remote = sys.shell("git remote -v").trim();
  const git_branch = sys.shell("git rev-parse --abbrev-ref HEAD").trim();
  const git_diff = sys.shell("git diff").trim();
  var fails = [];
  if(version_engineersdecor=="") fails.push("Could not determine 'version_engineersdecor' from gradle properties.");
  if(!gittags.length) fails.push("Version not tagged.");
  if(!gittags.filter(function(s){return s.indexOf(version_engineersdecor.replace(/[-]/g,""))>=0}).length) fails.push("No tag version not found matching the gradle properties version.");
  if(git_remote.replace(/[\s]/g,"").indexOf("git@github.com:stfwi/engineers-decor.git(push)") < 0) fails.push("Not the reference repository.");
  if((git_branch != "develop") && (git_branch != "master")) {
    fails.push("No valid branch for dist. (branch:'"+git_branch+"')");
  } else if((git_branch == "develop") && (version_engineersdecor.replace(/[^\w\.-]/g,"")=="")) {
    fails.push("Cannot make release dist on develop branch.");
  } else if((git_branch == "master") && (version_engineersdecor.replace(/[^\w\.-]/g,"")!="")) {
    fails.push("Cannot make beta dist on master branch.");
  }
  if(git_diff !== "") fails.push("Not everything committed to the GIT repository.");
  if((!fs.isfile("signing.jks")) || (!fs.isfile("signing.properties"))) fails.push("Jar signing files missing.");
  if(fails.length>0) {
    for(var i in fails) fails[i] = "  - " + fails[i];
    alert("Dist check failed");
    alert(fails.join("\n")+"\n");
    exit(1);
  }
};

tasks["sync-languages"] = function() {
 // @todo: has become easier but needs impl.
};

tasks["tabs-to-spaces"] = function() {
  var file_list = (function() {
    var ls = [];
    const ext = ['java'];
    for(var i in ext) ls = ls.concat(fs.find("./src", '*.'+ext[i]));
    for(var i in ls) ls[i] = ls[i].replace(/\\/g,"/");
    ls.sort();
    ls.push("readme.md");
    return ls;
  })();
  for(var file_i in file_list) {
    var file = file_list[file_i];
    var txt = fs.readfile(file);
    if(txt===undefined) throw new Error("Failed to read '" + file + "'");
    const txt_length = txt.length;
    txt = txt.replace(/[\t]/g,"  ");
    const n = txt.length - txt_length;
    if(n > 0) {
      print("File '" + file + "': Changed " + n + " tabs to 2 spaces." );
      fs.writefile(file, txt);
    }
  }
};

tasks["trailing-whitespaces"] = function() {
  var file_list = (function() {
    var ls = [];
    const ext = ['java','json','lang'];
    for(var i in ext) ls = ls.concat(fs.find("./src", '*.'+ext[i]));
    for(var i in ls) ls[i] = ls[i].replace(/\\/g,"/");
    ls.sort();
    ls.push("readme.md");
    return ls;
  })();
  for(var file_i in file_list) {
    var file = file_list[file_i];
    var txt = fs.readfile(file);
    if(txt===undefined) throw new Error("Failed to read '" + file + "'");
    const txt_length = txt.length;
    txt = txt.replace(/[\r\t ]+[\n]/g,"\n");
    const n = txt_length - txt.length;
    if(n > 0) {
      print("File '" + file + "': Fixed " + n + " lines with trailing whitespaces." );
      fs.writefile(file, txt);
    }
  }
};

tasks["version-check"] = function() {
  var version_minecraft="";
  var version_forge="";
  var version_engineersdecor="";
  fs.readfile("gradle.properties", function(line){
    if(line.search(/^[\s]*version_minecraft[\s]*=/i) >= 0) {
      version_minecraft = line.replace(/^[^=]+=/,"").trim();
    } else if(line.search(/^[\s]*version_forge[\s]*=/i) >= 0) {
      version_forge = line.replace(/^[^=]+=/,"").trim();
    } else if(line.search(/^[\s]*version_engineersdecor[\s]*=/i) >= 0) {
      version_engineersdecor = line.replace(/^[^=]+=/,"").trim();
    }
    return false;
  })
  const combined_version = version_minecraft + "-" + version_engineersdecor;
  var readme_version_found = fs.readfile("readme.md", function(line){
    var m = line.match(/^[\s]+-[\s]+v([\d]+[\.][\d]+[\.][\d]+[-][abrc][\d]+)/i);
    if((!m) || (!m.length) || (m.length < 2)) {
      m = line.match(/^[\s]+-[\s]+v([\d]+[\.][\d]+[\.][\d]+)/i);
      if((!m) || (!m.length) || (m.length < 2)) return false;
    }
    return m[1]==version_engineersdecor;
  });
  var ok=true;
  if(!readme_version_found) {
    alert("Version 'v" + version_engineersdecor + "' not found in the readme changelog.");
    ok = false;
  }
  if(!ok) {
    alert("Version data:");
    alert(" - combined_version  : '" + combined_version + "'");
    alert(" - version_forge     : '" + version_forge + "'");
    exit(1);
  }
};

tasks["dist"] = function() {
  function readme_history(file_path) {
    var readme = fs.readfile(file_path);
    if(!readme) throw new Error("Failed to load readme.md");
    readme = readme.split(/[\r]?[\n]/);
    while((readme.length > 0) && readme[0].search(/^## Revision history/i)<0) readme.shift();
    while((readme.length > 0) && readme[0].trim()=="") readme.shift();
    // revision history section
    if(!readme.length) throw new Error("Revision history section not found in readme");
    readme.shift();
    var end_of_history = readme.length;
    for(var i=0; i<readme.length; ++i) if(readme[i].search(/^---/) >= 0) { end_of_history=i; break; }
    if(end_of_history >= readme.length) throw new Error("Could not find the end-of-history header marker.");
    // remove empty lines, splitters
    while(readme.length >= end_of_history) readme.pop();
    while((readme.length >0) && (readme[readme.length-1].replace(/[\s-]/g,"")=="")) readme.pop();

    const min_indent = readme
      .map(function(s){return s.search(/[^\s]/)})
      .filter(function(e){return e>=0})
      .reduce(function(acc,e){return (e<acc)?e:acc});

    if(min_indent > 1) {
      for(var i in readme) { readme[i] = readme[i].substr(min_indent-2); }
    }
    return readme.join("\n");
  }
  const html = "<pre>\n" + (readme_history("readme.md").replace(/&/g, "&amp;").replace(/>/g, "&gt;").replace(/</g, "&lt;")) + "\n</pre>";
  fs.writefile("dist/revision-history.html", html);
};

tasks["update-json"] = function() {
  const root_dir = fs.cwd();
  function read_history() {
    var readme = fs.readfile(root_dir + "/readme.md");
    if(!readme) throw new Error("Failed to load readme.md");
    readme = readme.split(/[\r]?[\n]/);
    while((readme.length > 0) && readme[0].search(/^## Revision history/i)<0) readme.shift();
    // revision history section
    if(!readme.length) throw new Error("Revision history section not found in readme");
    readme.shift();
    var end_of_history = readme.length;
    for(var i=0; i<readme.length; ++i) if(readme[i].search(/^---/) >= 0) { end_of_history=i; break; }
    if(end_of_history >= readme.length) throw new Error("Could not find the end-of-history header marker.");
    // remove empty lines, splitters
    while(readme.length >= end_of_history) readme.pop();
    for(var i in readme) readme[i] = readme[i].replace(/[\s]+$/g,"").replace(/[\t]/g,"  ");
    readme = readme.filter(function(a){return a.replace(/[\s-]+/g,"")!="";});
    // condense multilines to single line entries for each fix or feature. ([A] ... [M] ...)
    for(var i=readme.length-1; i>0; --i) {
      var line = readme[i].replace(/^\s+/,"");
      if(line.search(/^[\[\-]/) < 0) {
        readme[i-1] += " " + line;
        readme[i] = "";
      }
    }
    readme = readme.filter(function(a){return a!="";});
    // Condense log entries sepatated with newlines to one line for each version
    for(var i=readme.length-1; i>0; --i) {
      var line = readme[i].replace(/^\s+/,"");
      if(line.search(/^-/) < 0) {
        readme[i-1] += "\n" + line;
        readme[i] = "";
      }
    }
    readme = readme.filter(function(a){return a!="";});
    // Separate versions.
    var history = {};
    for(var i in readme) {
      var line = readme[i].replace(/^[\sv-]+/g,"").trim();
      var ver = line.substr(0, line.search(" ")).trim().toLowerCase();
      var txt = line.substr(line.search(" ")).trim();
      if(history[ver] !== undefined) throw new Error("Double definition of version '" + ver + "' in the readme revision history.");
      history[ver] = txt;
    }
    return history;
  }
  var history = read_history();
  var latest_release = "";
  var latest_beta = "";
  for(var ver in history) { latest_beta=ver; break; }
  for(var ver in history) if(ver.search(/(rc|b|a)/) < 0) { latest_release=ver; break; }
  var update_json = {
    homepage: "https://www.curseforge.com/minecraft/mc-mods/engineers-decor/",
    "1.13.2": history,
    promos: {
      "1.13.2-recommended": latest_release,
      "1.13.2-latest": latest_beta,
    }
  }
  fs.mkdir(root_dir + "/meta");
  fs.writefile(root_dir + "/meta/update.json", JSON.stringify(update_json, null, 2));
};

const task_name = sys.args[0];
if((task_name===undefined) || (tasks[task_name])===undefined) {
  alert("No task ", task_name);
  exit(1);
} else {
  tasks[task_name]();
}
