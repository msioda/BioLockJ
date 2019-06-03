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

exports.createFullLaunchCommand = function(launchJSON, restartPath){//
  const execSync = require('child_process').execSync;
  const dockblj = path.join('..','script','dockblj');//relative path from webapp folder
  let command = [];
  command.push(dockblj.toString());
  Object.keys(launchJSON).forEach(key => {
    //if key not config, grab path.Dirname(launchJSON[key])

    //need to pass directories to map for all except -c and -i is already a directory
    if (key != 'c' && key != 'i'){// TODO: update this to handle '\' also incase someone is running windows
      command.push(`-${key}=${path.dirname(launchJSON[key])}`)
    }else{
    command.push(`-${key}=${launchJSON[key]}`);
    };
    // command.push(`-${key}=${launchJSON[key]}`);
  });
  command.push('-docker');
  if (restartPath !== undefined ){
    //note, change to make more universal
    command.push(`-r ${restartPath}`);
  }
  console.log('launch');
  console.log('full launch command: \n', command);
  return command;
}//end createFullLaunchCommand

exports.runLaunchCommand = function(command, eventEmitter) {
  const bljProjDir = process.env.BLJ_PROJ; //path to blj_proj
  const { spawn } = require('child_process');//for running child processes
  const first = command.shift();
  console.log(first);
  console.log(command);
  try {
    const child = spawn(first, command);
    child.stdout.on('data', function(data){
      eventEmitter.emit('log',data);
      console.log('child.stdout: ' + data);
    });
    child.stderr.on('data', function (data) {
        //throw errors
        eventEmitter.emit('log',data);
        console.log('child.stderr: ' + data);
    });
    child.on('error', function (data) {
        //throw errors
        eventEmitter.emit('log',data);
        console.log('child.err: ' + data);
    });
    child.on('close', function (code) {
        console.log('child process exited with code ' + code);
    });
    //child.unref();//to run in background
  } catch (e) {
    console.error(`launch error: ${e}`);
  }

}//end runLaunchCommand

exports.newestFileInDir = function(dirPath){

}

//shamelessly ripped from https://stackoverflow.com/questions/2727167/how-do-you-get-a-list-of-the-names-of-all-files-present-in-a-directory-in-node-j
// async version with basic error handling
exports.mapDir = function (currentDirPath, callback) {
    fs.readdir(currentDirPath, function (err, files) {
        if (err) {
            throw new Error(err);
        }
        files.forEach(function (name) {
            var filePath = path.join(currentDirPath, name);
            var stat = fs.statSync(filePath);
            if (stat.isFile()) {
                callback(filePath, stat);
            } else if (stat.isDirectory()) {
                walk(filePath, callback);
            }
        });
    });
}
