"use strict";
(function(constants){
  var me = {};


  /**
   * Determines a list of all textures in the given path as plain object,
   * where the keys are the unified texture path (e.g. "block/", not "blocks/"),
   * and the value an object containing file path, SHA1, size, etc.
   * @returns {object}
   */
  me.load_texture_data = function(textures_path) {
    const wd = fs.cwd();
    var data = {};
    try {
      if(!fs.chdir(textures_path)) throw new Error("Texture root path does not exist: '" + textures_path + "'");
      fs.find(".", '*.*', function(path) {
        const file = path.replace(/[\\]/g, "/").replace(/^\.\//,"");
        const unified_file = file.replace(/^blocks\//, "block/");
        data[unified_file] = { path:file, size:fs.size(file), sha:sys.hash.sha1(path, true) };
        return false;
      });
      return data;
    } finally {
      fs.chdir(wd);
    }
  }

  /**
   * Compares texture files and mcdata files two given assets paths, returns the
   * lists of both file trees and the differences as object.
   * @returns {object}
   */
  me.compare_textures = function(assets_path_a, assets_path_b) {
    const txpath_a = assets_path_a + "/" + constants.mod_registry_name() + "/textures";
    const txpath_b = assets_path_b + "/" + constants.mod_registry_name() + "/textures";
    const a = me.load_texture_data(txpath_a);
    const b = me.load_texture_data(txpath_b);
    const txpath_a_is112 = fs.isdir(txpath_a + "/blocks");
    const txpath_b_is112 = fs.isdir(txpath_b + "/blocks");
    const cmp = {a:{},b:{}};
    cmp.a.path = txpath_a;
    cmp.b.path = txpath_b;
    cmp.a.is112 = txpath_a_is112;
    cmp.b.is112 = txpath_b_is112;
    cmp.a.files = Object.assign({},a);
    cmp.b.files = Object.assign({},b);
    cmp.match = {}
    cmp.differ = {}
    cmp.onlyin_a = {}
    cmp.onlyin_b = {}
    for(var key in a) {
      if(b[key] === undefined) {
        cmp.onlyin_a[key] = a[key];
        continue;
      }
      if(a[key].sha === b[key].sha) {
        cmp.match[key] = a[key];
        b[key]=undefined; delete b[key];
      } else {
        cmp.differ[key] = { a:a[key], b:b[key] };
        b[key]=undefined; delete b[key];
      }
    }
    a = undefined;
    for(var key in b) {
      cmp.onlyin_b[key] = b[key];
    }
    b = undefined;
    return cmp;
  };

  /**
   * Loads all blockstate files in the given assets path, and returns the parsed JSON
   * data as plain object, where the keys are the blockstate names, and the value the
   * parsed JSON files.
   * @returns {object}
   */
  me.load_blockstates = function(assets_path) {
    const wd = fs.cwd();
    var data = {};
    try {
      if(!fs.chdir(assets_path+"/blockstates")) throw new Error("blockstates path not found in: '" + assets_path + "'");
      fs.find(".", '*.json', function(path) {
        const file = path.replace(/[\\]/g, "/").replace(/^\.\//,"");
        if(fs.basename(file) != fs.basename(file).toLowerCase()) throw new Error("Blockstate file must be lowercase: '"+file+"'"); // hard fail
        const blockstate_name = fs.basename(file).replace(/[\.]json/i, "");
        if(blockstate_name.search(/[^a-z0-9_]/) >= 0) throw new Error("Blockstate file name contains invalid characters: '"+file+"'"); // here, too
        var json = fs.readfile(path);
        if(json===undefined) throw new Error("Failed to read blockstate file '"+file+"' (could not open file)");
        try { json=JSON.parse(json); } catch(ex) { throw new Error("Failed to parse blockstate file '"+file+"' (invalid JSON)"); }
        data[blockstate_name] = {file:(assets_path+"/blockstates/"+file), data:json};
        return false;
      });
      return data;
    } finally {
      fs.chdir(wd);
    }
  };

  me.compare_blockstates = function(assets_path_a, assets_path_b) {
    const a = me.load_blockstates(assets_path_a);
    const b = me.load_blockstates(assets_path_b);
    const onlyin_a = {};
    const onlyin_b = {};
    for(var key in a) {
      if(b[key] === undefined) {
        onlyin_a[key] = a[key];
        continue;
      } else {
        b[key]=undefined; delete b[key];
      }
    }
    a = undefined;
    for(var key in b) {
      onlyin_b[key] = b[key];
    }
    b = undefined;
    return {
      onlyin_a: onlyin_a,
      onlyin_b: onlyin_b,
    }
  };

  Object.freeze(me);
  return me;
});