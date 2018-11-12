/**Functions for serverside js for BioLockJ gui
  *Author: Aaron Yerke
  *
*/

var fs = require('fs'),
  path = require('path');


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
  } finally {

  }

};

exports.progressStatus = function(dirPath){
  fs.watch(dirPath, (eventType, filename) => {
    console.log(`Filename: ${filename}, Event: ${eventType}`);
    console.log(fs.lstatSync('/Users/aaronyerke/Desktop/fodor_lab/blj_testing/' + filename).isDirectory());
  // could be either 'rename' or 'change'. new file event and delete
  // also generally emit 'rename'
  })
}

exports.saveConfigToLocal = function(configName, configText){
  if (!configName.endsWith('.properties')){
    configName = configName.concat('.properties')
  }
  const configPath = path.join('/config/', configName);
  console.log(configPath.toString());
  fs.writeFile(configPath, configText,function(err) {
    if(err) {
        return console.log(err);
    }
    console.log("The file was saved!");
  })
}

exports.buildPartialLaunchArgument = function (validConfig){
  partialLauchArgument = {};
  //config key : blj_argument
  //config Path will be built serverside
  const runtimeArguments = {
    'input.dirPaths' : 'inputDirPaths',
    'metadata.filePath' : 'metadataFilePath',
    'trimPrimers.filePath' : 'trimPrimersFilePath'
    //'project.configFile' : 'CONFIG_PATH',
    // TODO: Add -r and -p to arguements list
  };
  Object.keys(runtimeArguments).forEach(key => {
    if (Object.keys(validConfig).includes(key)){
      partialLauchArgument[runtimeArguments[key].toString()] = validConfig[key];
    };
  })
  return partialLauchArgument;
}

exports.createFullLaunchCommand = function(launchJSON){//
  //const bljProjDir = process.env.BLJ_PROJ; //path to blj_proj
  //const bljDir = process.env.BLJ;
  const dockblj = path.join('blj','script','dockblj')
  //console.log(dockblj);
  let command = [];
  //console.log(launchJSON);
  command.push(dockblj.toString());
  Object.keys(launchJSON).forEach(key => {
    //if key not config, grab path.Dirname(launchJSON[key])
    if (key != 'config' && key != 'inputDirPaths'){//need only the dir, not the file name
      launchJSON[key] = path.dirname(launchJSON[key]);
      command.push(`${key}=${launchJSON[key]}`)
    }else{
    command.push(`${key}=${launchJSON[key]}`);
    };
  });
  command.push('-docker');
  console.log('launch');
  console.log(command);
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
      console.log('child.stout: ' + data);
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
  } finally {

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
/*use with line like:
    walk('path/to/root/dir', function(filePath, stat) {
        // do something with "filePath"...
    });
*/
// fs.readdir('/Users/aaronyerke/Desktop/fodor_lab/blj_testing', (err, files) => {
//   files.forEach(file => {
//     console.log(file);
//   });
// })
