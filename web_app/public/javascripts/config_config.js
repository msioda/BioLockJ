var currentConfig = {};//IMPORTANT: This variable holds all of the selected configuations

class configO {//does currentConfig need to be an object
  constructor() {
    this.params = params;
    this.methods = methods;
  }
}

// function configValidation(config){//did I use this?
//   let formNames = [];
//   config.keys(obj).forEach(function(key,index) {
//     let keyForm = key.split('.')
//     console.log(keyForm);
//     if (!formNames.contains(keyForm)){
//       formNames.push(keyForm);
//     }
//   });//end forEach
// }//end configValidation

function loadLocalFile() {//loading from computer
  const file = document.getElementById("localFile").files[0];
  let localModules = [];
  if (file) {
    var reader = new FileReader();
    reader.onload = function(evt) {
      var provisionalConfigObject = {};
      provisionalConfigObject["project.configFile"] = file.name;
      provisionalConfigObject.modules = [];
      var lines = this.result.split('\n');
      for (var line = 0; line < lines.length; line++) {
        var lineSplit = lines[line].split("=");
        if (lines[line].slice(0, 11) === "#BioModule ") {
          console.log(lines[line].slice(11));
          localModules.push(lines[line].slice(11));
          provisionalConfigObject.modules.push(lines[line].slice(11));
        } else if (lineSplit.length >= 2 && !lines[line].startsWith('#')) { //if spliting at "=" is 2 or greater...
          provisionalConfigObject[lineSplit[0]] = lineSplit.slice(1).join('=');
        } else if (line[lines] == undefined) {
        } else {
          alert('Lines must start with "#", or have key/values seperated by "=". Please check your config form');
          return
        };
      }; //end for-loop
      try {
        console.log(provisionalConfigObject);
        sendConfigDataToForms(provisionalConfigObject, localModules);
        alert("document accepted");
        document.getElementById("openConfig").style.display = "none";
      } catch (err) {
        //alert(err + " possible invalid key");
        console.error(err);
      };
    }; //end reader.onload
    reader.onerror = function(evt) {
      console.error("An error ocurred reading the file", evt);
    };
    reader.readAsText(file, "UTF-8");
  } //end if (file)
}; //end loadLocalFile function

//function for parsing config objects in localStorage to forms
var sendConfigDataToForms = function(configObject, selectedModulesArray) {
  console.log(selectedModulesArray);
  myModules = orderModulesFromLocalFiles(selectedModulesArray, myModules);
  runModuleFunctions();//reorder module list elements to match the config

  currentConfig = configObject; //reset currentConfig to config object from memory
  /*For later, when I want to make this more readable*/
  const selects = Array.from(document.getElementsByTagName('select'));
  const inputs = Array.from(document.getElementsByTagName('input'));
  const texts = inputs.filter(inp => inp.type === 'text');
  const radios = inputs.filter(inp => inp.type === 'radio');
  const checkboxs = inputs.filter(inp => inp.type === 'checkbox');
  const numbers = inputs.filter(inp => inp.type === 'number');
  for (var key in configObject) {
    try {
      //first step, loop through modules and show them
      if (key == "modules" && configObject[key]) {
        var mods = configObject[key]; //could have said configObject["modules"]
        var domModule = document.getElementById('module');
        var domModuleLi = domModule.getElementsByTagName('li');
        for (var a = 0; a < mods.length; a++) {//for mod in saved mods
          for (var b = 0; b < domModuleLi.length; b++) {//for mod in mod li
            if (mods[a] == domModuleLi[b].innerHTML) {
              try{
              domModuleLi[b].click();//add('modChoosen');
            } catch(err) {
              alert(err);
            } finally{
              }
            };//end if
          };//end for-loop over domModuleLi
        }
      }//end modeule if statement
      else if (checkboxs.map(check => check.name).includes(key)){
        const checkTargets = checkboxs.filter(check => check.name == key);
        const cachedChecks = configObject[key].split(',');
        cachedChecks.forEach(val => {
          checkTargets.find(check => check.value === val).checked = true;
        });
      }else{
        var targetInput = document.getElementById(key);
        if (targetInput.tagName == 'SELECT'){
          var select = selects.find(sel => sel.id == key);
          var opt =
          Array.apply(null, select.options).find(option => option.value === currentConfig[key]);
          if (opt.value != undefined){ opt.setAttribute('selected', true); };//select opt if not undefined
          }
        else if (targetInput.type == 'text' || targetInput.type == 'number'){
          targetInput.value = configObject[key];
          }
        }//end else
      document.getElementById("mainMenu").style.display = "block";
    } catch (err) {
      alert(err + "\n problem with " + key + ".")
    } finally{
    }
  }; //end for-loop over configObject
}; //end sendConfigDataToForms

function validateConfig(){
  const configForm = document.getElementById('configForm');
  const requiredIntial = ['input.dirPaths', 'report.taxonomyLevels', 'script.permissions', 'script.defaultHeader'];
  const menuTabs = document.getElementsByClassName('tabcontent')
  for (let reqIni of requiredIntial){
    console.log(reqIni);
    if (!(reqIni in currentConfig)){

      console.log();
      console.log(document.getElementById(reqIni).parentNode.parentNode);
      return false;
    }
  }//end for loop
};
const inpDirPath = document.getElementById('input.dirPaths');

//   # Note, a “non-negative integer” accepts 0, a “positive integer” does not
//
// 2. Requires positive integer: script.numThreads, script.batchSize
//
// 3. demux.strategy is required, default value = do_not_demux (display as N/A)
// * key values: barcode_in_header, barcode_in_seq, id_in_header, do_not_demux
// * display values: Barcode in Header, Barcode in Sequence, ID in Header, N/A
//
// if( demux.strategy == barcode_in_header or barcode_in_seq) metadata.barcodeColumn is required.
//
//
// 4. project.env is required & should be a dropdown list:
// * key values: Cluster, aws, local
// * display values: Cluster, AWS, Local
//
// 5. project.logLevel is required & should be a dropdown list:
// * key values: DEBUG, INFO, WARN, ERROR
// * display values: Debug, Info, Warning, Error
//
// 6. Required for R modules: r.colorBase, r.colorHighlight, r.colorPalette, r.colorPoint, r.pch, r.plotWidth (positive integer), r.pvalCutoff, r.pValFormat, r.rareOtuThreshold (positive integer), r.timeout (positive integer), rStats.pAdjustMethod
//
//
// Also required for R Modules: rStats.pAdjustScope - should be a dropdown
// * key values: ATTRIBUTE, GLOBAL, LOCAL, TAXA
// * Display values: Attribute, Global, Local, Taxonomy Level
//
// 7. Required for BuildMdsPlots: rMds.numAxis (positive integer), rMds.distance, rMds.outliers
//
// 8. Required for Email: mail.encryptedPassword, mail.from, ail.smtp.auth, mail.smtp.host, mail.smtp.port, mail.to
//
// 9. One of these 2 are required as non-negative integer (not both) for Rarefier module: rarefier.max, rarefier.min
//
// 10. Required for RdpClassifier: rdp.minThresholdScore (positive integer
//
// 11. Required for SlimmClassifier: slimm.db, slimm.refGenomeIndex
//
// 12. Required for KrakenClassifier: kraken.db
//
// 13. Required for any QiimeClassifer (closed, deNovo, open)
//
// 14. metadata.commentChar should be of length = 1 (if it exists)
//
// 15. Required for TrimPrimers: trimPrimers.filePath
//
// These required non-negative integers: report.minOtuCount, report.minOtuThreshold
//
// Correction on #4 below (the auto-correct capitalized the C in cluster)
//
// 4. project.env is required & should be a dropdown list:
// * key values: cluster, aws, local
// * display values: Cluster, AWS, Local
//
// Also, if ( project.env == “cluster” ) require cluster.batchCommand, cluster.host, cluster.jobHeader, cluster.classifierHeader
//
// Config fields: cluster.numClassifierThreads, input.seqMaxLen, input.seqMinLen  are not required, but only accept positive integers

function saveConfigParamsForm(event){
  //event.preventDefault()
  const configForm = document.getElementById('configForm');
  const configFile = document.getElementById('project.configFile');
  if (configFile.value == ""){
    let now = new Date();
    let c = now.toString().replace(' ', '_');
    console.log(c);
    configFile.value = c;
  }
  const configParaForm = new FormData(configForm);
  let input = [], value = [];
  for(var pair of configParaForm.entries()) {
    console.log(`${pair[0]} ${pair[1]}`);
    if (pair[1] != ''){
      input.push(pair[0]);
      value.push(pair[1]);
    };
  };
  input.forEach(key => currentConfig[key] = '');
  for (let i = 0; i < input.length;i++){
      if (!currentConfig[input[i]]){
        currentConfig[input[i]] = value[i];
      }else{
        currentConfig[input[i]] = currentConfig[input[i]].concat(',',value[i]);
      }
    }
  modulesToCurrentConfig();
  localStorage.setItem(currentConfig['project.configFile'].toString(), JSON.stringify(currentConfig));
  console.dir(currentConfig);
};//end saveConfigParams

function modulesToCurrentConfig() {
  mods = document.getElementById('module').getElementsByTagName('li');
  selectedMods = [];
  for (var i = 0; i < mods.length; i++) {
    if (mods[i].classList.contains('modChoosen') && mods[i].disabled != true) {
      selectedMods.push( mods[i].innerHTML)
    };
  };
  if (selectedMods.length > 0) {
    currentConfig['modules'] = selectedMods;
  }else{delete currentConfig['modules']}
  localStorage.setItem(currentConfig['project.configFile'].toString(), JSON.stringify(currentConfig));
};

//Function for creating downloadable config file
(function() {
  makeTextFile = function() {
    var text = "";
    textFile = null;
    //add modules to config first
    if (currentConfig["modules"] != null){
      for (var i = 0; i < currentConfig["modules"].length; i++) {
        text += '#BioModule '.concat(currentConfig["modules"][i],"\n");
      }
    };
    //for non_module
    for (var key in currentConfig) {
      console.log(key);
      if (currentConfig.hasOwnProperty(key)) { //only lets keys that are user inputed pass
        if (key == "modules" || key == "project.configFile") {// skipping project.configFile and modules
        } else if (key.toString() != "project.configFile" || key != "modules") { //project.configFile doesn't go inside the document
          text += key.concat("=", currentConfig[key], "\n");
        }
      }
    }
    var data = new Blob([text], {
      type: 'text/plain'
    });
    // If we are replacing a previously generated file we need to manually revoke the object URL to avoid memory leaks.
    if (textFile !== null) {
      window.URL.revokeObjectURL(textFile);
    }
    textFile = window.URL.createObjectURL(data);
    return textFile;
  };


function buildLaunchArgument(validConfig){
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

//Adding all eventlisteners
for (const launch of document.getElementsByClassName("launchBlj")) {
  launch.addEventListener("click", function(){
    // TODO: make a loop to add all forms to current config and do validation
    try {
      //tabForm.forEach( ele => ele.submit());
      saveConfigParamsForm();
      var request = new XMLHttpRequest();
      request.open('POST', '/launch', true);
      request.setRequestHeader("Content-Type", "application/json");
      request.send(JSON.stringify({
        config : currentConfig,
        partialLaunchArg : buildLaunchArgument(currentConfig),
        //additional parameters for launch
      }));
      console.log('launch sent');
      request.onreadystatechange = function() {
      if (request.readyState == XMLHttpRequest.DONE) {
        console.log(request.responseText);
        window.location = '/progress';
        }
      }
      console.log(request.responseText);
    } catch (e) {
      alert(e)
    }
  });//end eventlistener
};//end forloop

//gets all buttons with create class
var createDownload = document.getElementsByClassName('createDownload');
for (var i = 0; i < createDownload.length; i++) {
  createDownload[i].addEventListener('click', function() {
    event.preventDefault()
    var links = document.getElementsByClassName('downloadlink');
    for (var a = 0; a < links.length; a++) {
      let link = links[a];
      link['download'] = currentConfig["project.configFile"] + '.properties';
      link.href = makeTextFile();
      link.style.display = 'block';
      };
    }, false);
  };
})();

//for autosave


Array.from(document.getElementById('configForm').getElementsByTagName('input'))
  .forEach(inp => inp.addEventListener('blur', saveConfigParamsForm()));
