"use strict";
(function(){
  var c = {};
  c.modid = "engineersdecor";
  c.mod_registry_name = function() { return c.modid; }
  c.local_assets_root = function() { return "src/main/resources/assets/" + c.mod_registry_name(); }
  c.reference_repository = function() { return "git@github.com:stfwi/engineers-decor.git"; }
  c.gradle_property_modversion = function() { return "version_engineersdecor"; }
  c.gradle_property_version_minecraft = function() { return "version_minecraft"; }
  c.gradle_property_version_forge = function() { return "version_forge"; }
  c.project_download_inet_page = function() { return "https://www.curseforge.com/minecraft/mc-mods/engineers-decor/"; }
  c.options = {
    without_ref_repository_check: true
  };
  c.languages = {
    "en_us": { code:"en_us", name:"English", region:"United States" },
    "de_de": { code:"de_de", name:"German", region:"Germany" },
    "ru_ru": { code:"ru_ru", name:"Russian", region:"Russia" },
    "zh_cn": { code:"zh_cn", name:"Chinese", region:"China" }
  }
  Object.freeze(c.languages);
  Object.freeze(c);
  return c;
});
