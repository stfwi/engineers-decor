#!/usr/bin/djs
// run from tasks.js directory
"use strict";
(function(constants, libassets){
  const me = {};
  const modid = constants.mod_registry_name();
  const assets_root = constants.local_assets_root();

  const create_item_model = function(prefix, texture_prefix) {
    const model = {
      parent: modid+":block/slab/generic/slab_inventory_model",
      textures: { all: modid+":block/"+texture_prefix+"0" }
    }
    const path = "models/item/"+prefix+"_slab.json";
    if(!fs.writefile(path, JSON.stringify(model))) {
      throw new Error("Failed to write item model file '"+ path +"'");
    }
  };

  const create_block_models = function(prefix, texture_prefix) {
    for(var parts=0; parts<3; ++parts) {
      for(var tvariant=0; tvariant<4; ++tvariant) {
        const model = {
          parent: modid+":block/slab/generic/slab_s"+parts+"_model",
          textures: { all: modid+":block/"+texture_prefix+tvariant }
        }
        const path = "models/block/slab/specific/"+prefix+"_slab_s"+parts+"v"+tvariant+"_model.json";
        if(!fs.writefile(path, JSON.stringify(model))) {
          throw new Error("Failed to write model file '"+ path +"'");
        }
      }
    }
  };

  const create_blockstate = function(prefix) {
    var variants = {};
    for(var parts=0; parts<3; ++parts) {
      for(var tvariant=0; tvariant<4; ++tvariant) {
        variants[ ("parts="+parts+",tvariant="+tvariant).replace(/[\s]/g,"") ] = {
          model: (modid+":block/slab/specific/"+prefix+"_slab_s"+parts+"v"+tvariant+"_model").replace(/[\s]/g,"")
        }
      }
    }
    const path = "blockstates/"+prefix+"_slab.json";
    if(!fs.writefile(path, JSON.stringify({variants:variants},null,1))) throw new Error("Failed to write blockstate '"+path+"'");
    return path;
  };

  me.create = function(prefixes) {
    const here = fs.cwd()
    const registry_name_prefix = prefixes.name_prefix;
    const texture_prefix = prefixes.texture_prefix;
    if(!fs.chdir(assets_root)) throw new Error("Could not switch to assets root folder: '" + assets_root + "'");
    try {
      create_block_models(registry_name_prefix, texture_prefix);
      create_item_model(registry_name_prefix, texture_prefix);
      create_blockstate(registry_name_prefix, texture_prefix);
    } finally {
      fs.chdir(here);
    }
  }
  Object.freeze(me);
  return me;
});
