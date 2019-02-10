#!/usr/bin/djs
"use strict";
const root_dir = fs.realpath(fs.dirname(sys.script)+"/..");

function read_history() {
  var readme = fs.readfile(root_dir + "/readme.md");
  if(!readme) throw new Error("Failed to load readme.md");
  readme = readme.split(/[\r]?[\n]/);
  while((readme.length > 0) && readme[0].search(/^## Revision history/i)<0) readme.shift();
  // revision history section
  if(!readme.length) throw new Error("Revision history section not found in readme");
  readme.shift();
  var end_of_history = readme.length;
  for(var i=0; i<readme.length; ++i) if(readme[i].search(/^#/) >= 0) { end_of_history=i; break; }
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
  homepage: "https://www.curseforge.com/minecraft/mc-mods/redstone-gauges-and-switches/",
  "1.12.2": history,
  promos: {
    "1.12.2-recommended": latest_release,
    "1.12.2-latest": latest_beta,
  }
}

fs.writefile(root_dir + "/meta/update.json", JSON.stringify(update_json, null, 2));
