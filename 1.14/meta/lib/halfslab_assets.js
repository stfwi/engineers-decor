#!/usr/bin/djs
// run from tasks.js directory
"use strict";
(function(constants, libassets){
  const me = {};
  const modid = constants.mod_registry_name();
  const assets_root = constants.local_assets_root();
  const hexchar = ['0','1','2','3','4','5','6','7','8','9','a','b','c','d','e']; // ('a'+parts) won't work so here we go

  const create_item_model = function(prefix, texture) {
    const model = {
      parent: modid+":block/slab/generic/halfslab_inventory_model",
      textures: { all: texture }
    }
    const path = "models/item/halfslab_"+prefix+".json";
    if(!fs.writefile(path, JSON.stringify(model))) {
      throw new Error("Failed to write item model file '"+ path +"'");
    }
  };

  const create_block_models = function(prefix, texture) {
    for(var parts=0; parts<15; ++parts) {
      const model = {
        parent: modid+":block/slab/generic/halfslab_s"+hexchar[parts]+"_model",
        textures: { all:texture }
      }
      const path = "models/block/slab/specific/halfslab_"+prefix+"_s"+hexchar[parts]+"_model.json";
      if(!fs.writefile(path, JSON.stringify(model))) {
        throw new Error("Failed to write model file '"+ path +"'");
      }
    }
  };

  const create_blockstate = function(prefix) {
    var variants = {};
    for(var parts=0; parts<15; ++parts) {
      variants[ ("parts="+parts).replace(/[\s]/g,"") ] = {
        model: (modid+":block/slab/specific/halfslab_"+prefix+"_s"+hexchar[parts]+"_model").replace(/[\s]/g,"")
      }
    }
    const path = "blockstates/halfslab_"+prefix+".json";
    if(!fs.writefile(path, JSON.stringify({variants:variants},null,1))) throw new Error("Failed to write blockstate '"+path+"'");
    return path;
  };

  me.create = function(data) {
    const here = fs.cwd()
    const registry_name_prefix = data.name_prefix;
    const texture = data.texture;
    if(!fs.chdir(assets_root)) throw new Error("Could not switch to assets root folder: '" + assets_root + "'");
    try {
      create_block_models(registry_name_prefix, texture);
      create_item_model(registry_name_prefix, texture);
      create_blockstate(registry_name_prefix, texture);
    } finally {
      fs.chdir(here);
    }
  }
  Object.freeze(me);
  return me;
});
