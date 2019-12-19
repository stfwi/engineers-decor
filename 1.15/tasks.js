#!/usr/bin/djs
// Note for reviewers/clones: This file is a auxiliary script for my setup. It's not needed to build the mod.
"use strict";
const constants  = include("../meta/lib/constants.js")();
constants.options.without_ref_repository_check = true;
const libtask    = include("../meta/lib/libtask.js")(constants);
const liblang    = include("../meta/lib/liblang.1.13.js")(constants); // 1.14 lang same as 1.13
const libassets  = include("../meta/lib/libassets.js")(constants);
const libtask114 = include("../meta/lib/libtask.1.14.js")(constants, libassets, liblang);
var tasks = {};

tasks["sync-languages"] = function() {
  liblang.sync_languages();
};

tasks["assets"] = libtask114.stdtasks["assets"];
tasks["datagen"] = libtask114.stdtasks["datagen"];

tasks["create-slab-assets"] = function() {
  const libassets = include("../meta/lib/libassets.js")(constants);
  const slab_assets = include("meta/lib/slab_assets.js")(constants, libassets);
  const block_prefixes = [
    { name_prefix:"clinker_brick", texture_prefix:"clinker_brick/clinker_brick_texture" },
    { name_prefix:"clinker_brick_stained", texture_prefix:"clinker_brick/clinker_brick_stained_texture" },
    { name_prefix:"panzerglass", texture_prefix:"glass/panzerglass_block_texture" },
    { name_prefix:"rebar_concrete", texture_prefix:"concrete/rebar_concrete_texture" },
    { name_prefix:"rebar_concrete_tile", texture_prefix:"concrete/rebar_concrete_tile_texture" },
    { name_prefix:"slag_brick", texture_prefix:"slag_brick/slag_brick_texture" },
  ];
  for(var i in block_prefixes) slab_assets.create(block_prefixes[i]);
}

tasks["create-half-slab-assets"] = function() {
  const libassets = include("../meta/lib/libassets.js")(constants);
  const halfslab_assets = include("meta/lib/halfslab_assets.js")(constants, libassets);
  const modid = constants.mod_registry_name();
  const block_data = [
    { name_prefix:"rebar_concrete", texture:modid+":block/concrete/rebar_concrete_texture0" },
    { name_prefix:"concrete", texture:modid+":block/ieoriginal/ie_stone_decoration_concrete" },
    { name_prefix:"treated_wood", texture:"immersiveengineering:blocks/treated_wood" },
    { name_prefix:"sheetmetal_iron", texture:"immersiveengineering:blocks/sheetmetal_iron" },
    { name_prefix:"sheetmetal_steel", texture:"immersiveengineering:blocks/sheetmetal_steel" },
    { name_prefix:"sheetmetal_copper", texture:"immersiveengineering:blocks/sheetmetal_copper" },
    { name_prefix:"sheetmetal_gold", texture:"immersiveengineering:blocks/sheetmetal_gold" },
    { name_prefix:"sheetmetal_aluminum", texture:"immersiveengineering:blocks/sheetmetal_aluminum" },
 // { name_prefix:"clinker_brick", texture:modid+":block/clinker_brick/clinker_brick_texture0" }
  ];
  for(var i in block_data) halfslab_assets.create(block_data[i]);
}

libtask.run(tasks, sys.args);
