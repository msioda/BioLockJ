/**
  *Functions for retrieving and modifying pipelines
  *Author: Aaron Yerke
  *
*/

const fs = require('fs'),
  path = require('path'),
  errorLogger = require('./errorLogger.js');

//returns
exports.retrieveAllPipelines = function(req, res, next) {
  console.log('retrieveProjects');
  try {
    let names = [];
    let descrips = [];
    let paths = [];
    let complete = [];
    fs.readdir(path.join( '/', 'pipelines' ), (err, files) => {
      if (err) {
        console.error(err);
        accessLogStream.write(e.stack + '\n');
      }
      let completePosition = 0;//variable for
      files.forEach( file => {
        const checkFile = fs.lstatSync(path.join('/','pipelines',file));
        if (checkFile.isDirectory()) {
          complete[completePosition] = false;
          //go into folder
          const nestedFolderFiles = fs.readdirSync(path.join('/', 'pipelines', file));

          //get master config and read description
          for (var i = 0; i < nestedFolderFiles.length; i++) {
            console.log('nestedFolderFiles[i]: ', nestedFolderFiles[i]);
            if (nestedFolderFiles[i] == 'biolockjComplete') {
              complete[completePosition] = true;
            }
            if (nestedFolderFiles[i].startsWith('MASTER_') && nestedFolderFiles[i].endsWith('.properties')) {
              // console.log('nestedFolderFiles[i] MASTER_', nestedFolderFiles[i]);
              const propFilePath = path.join('/', 'pipelines', file, nestedFolderFiles[i]);
              // console.log('propFilePath: ', propFilePath);
              const propFile = fs.readFileSync(propFilePath, 'utf8').split('\n');

              let projDescrp = 'Project description is empty';

              for (let a = 0; a < propFile.length; a++) {
                if (propFile[i].startsWith('pipeline.description=')) {
                  console.log(propFile[a].slice(20));
                  projDescrp = propFile[a].slice(20);
                }
              }//end propFile for loop
              names.push(file);
              descrips.push(projDescrp);
              paths.push(propFilePath);
            }
          }//end nestedFolderFiles for loop
          completePosition += 1;
        };//end if dir
      });
      // console.log(projects);
      console.log('descrip: ', descrips);
      res.setHeader("Content-Type", "text/html");
      res.write(JSON.stringify({
        names : names,
        descrips : descrips,
        paths : paths,
        complete : complete,
      }));
      res.end();
    });
  } catch (e) {
    console.error(e);
    errorLogger.writeError(e.stack);
  }finally {
    // next()
  }
}