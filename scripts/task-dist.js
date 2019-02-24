#!/usr/bin/djs
"use strict";
if((!fs.chdir(fs.dirname(fs.realpath(sys.script))+"/..")) || (!fs.isdir(".git"))) throw new Error("Failed to switch to mod source directory.");

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
  for(var i=0; i<readme.length; ++i) if(readme[i].search(/^#/) >= 0) { end_of_history=i; break; }
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
