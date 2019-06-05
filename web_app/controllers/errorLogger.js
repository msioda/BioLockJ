/**
  *Functions for logging errors
  *Author: Aaron Yerke
  *
*/
'use strict'

const fs = require('fs'),
  path = require('path');

let accessLogStream = 'no errors yet';

//for error reports
exports.writeError = function(er){
  try {
    if (accessLogStream === 'no errors yet'){
      console.log('no errors yet');
      accessLogStream = fs.createWriteStream(createLogFile(), { flags: 'a' });
      accessLogStream.write(er + '\n');
    }
    else{
      console.log('logging error');
      accessLogStream.write(er + '\n');
    }
  } catch (e) {
    console.error(e.stack);
    accessLogStream.write(e.stack + '\n');
  }
}

function createLogFile(){//creates file and returns file name
  let now = new Date();
  let year = now.getYear() + 1900;
  let month = now.toLocaleString("en-us", {
    month: "short"
    });
  let day = now.getDate();
  let hour = now.getHours();
  let min = now.getMinutes();
  let c = ['webapp_log', year, month, day, hour, min].join('_');
  let p = path.join('/','log', c)
  fs.writeFile(p, "Log for BLJ webapp\n", function(err) {
    if(err) {
      accessLogStream.write(err);
      return console.log(err);
    }
    console.log(`created ${c}`);
});
  return p;
}// end of function loggerFileName()