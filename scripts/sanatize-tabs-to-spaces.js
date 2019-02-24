#!/usr/bin/djs
if((!fs.chdir(fs.dirname(fs.realpath(sys.script))+"/..")) || (!fs.isdir("src"))) throw new Error("Failed to switch to mod source directory.");

var file_list = (function() {
  var ls = [];
  const ext = ['java','lang'];
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
