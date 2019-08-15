/**Functions for serverside js for BioLockJ gui
  *Author: Aaron Yerke
  *
*/

const fs = require('fs'),
  path = require('path'),
  BLJ_CONFIG = process.env.BLJ_CONFIG;

/**parseBljJson turns a config json into a string in flat file format.*/
exports.formatAsFlatFile = function(modules, paramKeys, paramValues){
  var text = "";
  textFile = null;
  try {
    //add modules to config first
    if ( modules != null && modules.length > 0 ){
      for (let i = 0; i < modules.length; i++) {
        text += '#BioModule '.concat(modules[i],"\n");;
      }
      //console.log(text);
    };
    //for non_module
    if ( paramKeys != null && paramKeys.length > 0 && paramValues != null && paramValues.length > 0 ){
      for (let i = 0; i < paramKeys.length; i++){
        text += paramKeys[i].concat("=", paramValues[i], "\n");
      }
    }
    return text;
  } catch (e) {
    console.log(e);
  }
};

exports.progressStatus = function(dirPath){
  fs.watch(dirPath, (eventType, filename) => {
    console.log(`Filename: ${filename}, Event: ${eventType}`);
  // could be either 'rename' or 'change'. new file event and delete
  // also generally emit 'rename'
  })
}

exports.saveConfigToLocal = function(configName, configText){
  if (!configName.endsWith('.properties')){
    configName = configName.concat('.properties')
  }
  const configPath = path.join(BLJ_CONFIG, configName);
  //console.log(configPath.toString());
  fs.writeFile(configPath, configText,function(err) {
    if(err) {
      return console.log(err);
    }
    console.log("The file was saved!");
  })
}
