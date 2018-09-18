/**Functions for serverside js for BioLockJ guide
  *Author: Aaron Yerke
  *
*/

var fs = require('fs'),
  path = require('path');


/**parseBljJson turns a config json into a string in flat file format.
*/
exports.parseBljJson = function(currentConfig){
  var text = "";
  textFile = null;
  //add modules to config first
  if (currentConfig["modules"] != null){
    for (var i = 0; i < currentConfig["modules"].length; i++) {
      text += currentConfig["modules"][i].concat("\n");
    }
  };
  //for non_module
  for (var key in currentConfig) {
    //console.log(key);
    if (currentConfig.hasOwnProperty(key)) { //only lets keys that are user inputed pass
      if (key == "modules" || key == "project.configFile") {// skipping project.configFile and modules
      } else if (currentConfig[key] != '') { //project.configFile doesn't go inside the document
        text += key.concat("=", currentConfig[key], "\n");
      }
    }
  }
  return text;
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

exports.buildLaunchArgument = function (validConfig){
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
  const bljProjDir = process.env.BLJ_PROJ; //path to blj_proj
  const bljDir = process.env.BLJ;
  const dockblj = path.join(bljDir,'dist','dockblj')
  //console.log(dockblj);
  let launch = dockblj.toString();
  //console.log(launchJSON);
  Object.keys(launchJSON).forEach(key => {
    //if key not config, grab path.Dirname(launchJSON[key])
    if (key != 'config' && key != 'inputDirPaths'){//need only the dir, not the file name
      launchJSON[key] = path.dirname(launchJSON[key]);
    }
    console.log(` ${key}=${launchJSON[key]}`);
    launch = launch + ` ${key}=${launchJSON[key]}`;
  });
  console.log('launch');
  console.log(launch);
  return launch;
}//end createFullLaunchCommand

exports.runLaunchCommand = function(command) {
  const { spawn } = require('child_process');//for running child processes
  try {
    const child = spawn(command , (error, stdout, stderr) => {
            console.log(`launch stdout ${stdout}`);
            console.log(`$launch stderr {stderr}`);
            if (error !== null) {
                console.log(`exec error: ${error}`);
            }
        });
    //child.chdir('/config/');
    // child.stdout.on('data',
    //     function (data) {
    //         console.log('ls command output: ' + data);
    //     });
    // child.stderr.on('data', function (data) {
    //     //throw errors
    //     console.log('stderr: ' + data);
    // });
    // child.on('close', function (code) {
    //     console.log('child process exited with code ' + code);
    // });
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

exports.testing = function(currentConfig){
  console.log('asfdsdklfjadslfjsadlk;fjsdl;kfjasdlfkjaf;asldfjasdlkfjas;dfjasd;lfjkasd;f');
}
