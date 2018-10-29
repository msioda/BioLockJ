var currentConfig = {};//IMPORTANT: This variable holds all of the selected configuations

// class configO {//does currentConfig need to be an object
//   constructor() {
//     this.params = params;
//     this.methods = methods;
//   }
// }

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
  const requiredParams = new Map(Object.entries({
    'input.dirPaths' : 'what is your sequence input directory path?',
    'report.taxonomyLevels' : 'What taxonomy levels would you like in the report?',
    'script.permissions' : 'what are the script permissions?',
    'script.defaultHeader' : 'what are the script default headers?',
    'demux.strategy' : 'What demultiplexing statagy do you want to use?',
    'project.env' : 'In which enviroment do you wish to run this project?',

  }));

  const reqForR = new Map(Object.entries({//R module dependencies
    'r.colorBase' : 'Please choose your R plot base color.',
    'r.colorHighlight' : 'Please choose your R plot highlight color.',
    'r.colorPalette' : 'Please choose your R plot color palette.',
    'r.colorPoint' : 'Please choose your R plot point color.',
    'r.pch' : 'Please choose your R point size.',
    'r.plotWidth' : 'Please choose your R plot width (positive integer).',
    'r.pvalCutoff' : 'Please choose your p-value cut off (alpha) for your R statistics.',
    'r.pValFormat' : 'Please choose your p-value format for your R reports.',
    'r.rareOtuThreshold' : 'Please choose your rare OTU threshold (positive integer).',
    'r.timeout' : 'Please set your R timeout threshold (positive integer).',
    'rStats.pAdjustMethod' : 'Please choose your p-value adjust method.',
    'rStats.pAdjustScope' : 'Please choose your p-value adjust scope.'
    }));
  const reqForEmail = new Map(Object.entries({
    'mail.encryptedPassword' : 'In order to receive an Emailed report, please provide your encrypted password.',
    'mail.from' : 'Please choose the Email address from which you want to recieve your Emailed report.',
    'ail.smtp.auth' : 'Please provide SMTP Authentication.',
    'mail.smtp.host' : 'Please provide your SMTP host.',
    'mail.to' : 'Please provide the Email recipient.',
    'mail.smtp.port' : 'Please provide the SMTP port.'
    }));

  const reqForMdsPlots = new Map(Object.entries({
    'rMds.numAxis' : 'Please provide the number of axes for the MDS plot.',
    'rMds.distance' : 'Please provide the number of axes for the MDS plot.',
    'rMds.outliers' : 'Please choose the Email address from which you want to recieve your Emailed report.',
    }));

  const reqForRdpClassifier = new Map(Object.entries({
    'rdp.minThresholdScore' : 'Please provide the RDP minium threshold score.'
    }));

  const reqForSlimmClassifier = new Map(Object.entries({
  'slimm.db' : 'Please provide a Slimm database.',
  'slimm.refGenomeIndex' : 'Please provide a reference genome index for Slimm.'
  }));

  const reqForKrakenClassifier = new Map(Object.entries({
    'kraken.db' : 'Please provide a Kraken database.',
    }));

  const menuTabs = document.getElementsByClassName('tabcontent');
  const menuTabButtons = document.getElementsByClassName('tablinks');

  function getParentDiv(nodeId){
    let parentDiv;
    var node = document.getElementById(nodeId);
    let counter = 4;
    while (parentDiv == undefined && counter > 0){
      console.log(node);
      if (node.parentNode.tagName == 'DIV'){
        parentDiv = node.parentNode;
        return parentDiv;
      }
      if (counter === 0){
        alert(`Check for developer bug found in getParentDiv: ${node}`)
      }
      node = node.parentNode;
      counter--
    }//end while
  }//end getParentDiv

  function getCurrentMenuTab(){//shows the currently viewed tab
    for (let i = 0; i < menuTabs.length; i++) {
      if (menuTabs[i].style.display == 'block'){
        return menuTabs[i];
      }
    }//end for forloop
    alert('problem with getCurrentMenuTab')
  };//end getCurrentMenuTab

  function highlightRequiredParam(nodeId){
    const target = document.getElementById(nodeId);
    //found here: https://css-tricks.com/restart-css-animation/
    if (target.classList.contains('missingParameterOnValidation')){
      target.classList.remove('missingParameterOnValidation');
      void target.offsetWidth;
      target.classList.add('missingParameterOnValidation');
    }else{
      target.classList.add('missingParameterOnValidation');
    }
  }//end highlightRequiredParam

  function dependenciesPresent(dependencyMap){
    const menuTabsArray = Array.from(menuTabs);
    for (let para of dependencyMap.keys()){
      console.log(para);
      if (!(para in currentConfig)){
        //resetAnimation(requiredParamIds[r]);
        console.log(para);
        const currentMenuTab = getCurrentMenuTab();
        console.log(menuTabsArray.indexOf(currentMenuTab));
        // console.log(currentMenuTab);
        currentMenuTab.style.display = 'none';
        menuTabButtons[menuTabsArray.indexOf(getParentDiv(para))].click();
        //getParentDiv(para).style.display='block';
        alert('Required information missing: '.concat(dependencyMap.get(para)))//change this to modal later
        highlightRequiredParam(para);
        return false;
      }//end if
    }//end for loop
  }

  //First check: check for required parameters
  if (dependenciesPresent(requiredParams) == false){
    return false;
  }
  // for (let para of requiredParams.keys()){
  //   console.log(para);
  //   if (!(para in currentConfig)){
  //     //resetAnimation(requiredParamIds[r]);
  //     console.log(para);
  //     const currentMenuTab = getCurrentMenuTab();
  //     console.log(currentMenuTab);
  //     currentMenuTab.style.display = 'none';
  //     getParentDiv(para).style.display='block';
  //     alert('Required information missing: '.concat(requiredParams.get(para)))//change this to modal later
  //     highlightRequiredParam(para);
  //     return false;
  //   }//end if
  // }//end for loop

  if ( ['barcode_in_header', 'barcode_in_seq'].includes(currentConfig['demux.strategy'])){
    if (!currentConfig['metadata.barcodeColumn'] || currentConfig['metadata.barcodeColumn'] == ''){
      const currentMenuTab = getCurrentMenuTab();
      console.log(currentMenuTab);
      currentMenuTab.style.display = 'none';
      getParentDiv('metadata.barcodeColumn').style.display='block';
      alert('If demultiplex strategy is either "barcode in header", or "barcode in sequence", then the header of the metadata barcode column must be named.')//change this to modal later
      highlightRequiredParam('metadata.barcodeColumn');
      return false;
    }//end nested if
  }//end demux.strategy if

  //check for module dependencies
  if (!(currentConfig['modules'])){
    alert('Please select at least one module.')
    return false;
  }
  for (let mod of currentConfig['modules']){
    //check for demendencies
    console.log('inside switch ',mod);
    // NOTE: When I change the format of the module li's this will change...
    let shortMod = mod.slice('biolockj.module.'.length);
    console.log(shortMod);
    switch(shortMod) {
    case 'r.BuildMdsPlots': case 'r.BuildOtuPlots': case 'r.BuildPvalHistograms': case 'r.CalculateStats':
      if (dependenciesPresent(reqForR) == false){
        return false;
      }
      break;
    case 'report.Email':
      if (dependenciesPresent(reqForEmail) == false){
        return false;
      }
      break
    case 'seq.rarefier':
      // NOTE: ASK WHAT to do if both are there.
      if (!(currentConfig['rarefier.max'] || currentConfig['rarefier.min'])){
        const currentMenuTab = getCurrentMenuTab();
        currentMenuTab.style.display = 'none';
        getParentDiv('rarefier.max').style.display='block';
        alert('A maximum or minimum value is required for the rarifier.');
        highlightRequiredParam('rarefier.min');
        highlightRequiredParam('rarefier.max');
        return false;
      }
         break;
    case 'classifier.r16s.RdpClassifier':
      if (dependenciesPresent(reqForRdpClassifier) == false){
        return false;
      }
      break
    case 'classifier.wgs.KrakenClassifier':
      if (dependenciesPresent(reqForKrakenClassifier) == false){
        return false;
      }
      break
    case 'classifier.wgs.SlimmClassifier':
      if (dependenciesPresent(reqForSlimmClassifier) == false){
        return false;
      }
      break
    default: //optional
    //statements
    }//end switch

    const modules = currentConfig['modules'];

// If module = biolockj.module.implicit.parser.*, no biolockj.module.seq.* or biolockj.module.classifier.* modules can come after
// IF email is included, don't worry about the order, we automatically re-order so it runs last if its found anywhere
// If module = biolockj.module.implicit.report, *no biolockj.module.seq.* or biolockj.module.classifier. *or biolockj.module.implicit.parser.* modules can come after
// oops, I meant If module = biolockj.module.report.*  (no implicit pacakge)
// If module = biolockj.module.r. *no biolockj.module.seq.* or biolockj.module.classifier. *or biolockj.module.implicit.parser.* modules can come after
// thats everything

    return true;
  }//end for loop


};//end validation


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
    let year = now.getYear() + 1900;
    let month = now.getMonth() + 1;
    let day = now.getDay();
    let c = 'Untitled_BLJ_project_'.concat(year,'/',month,'/',day,);
    //console.log(c);
    configFile.value = c;
  }
  const configParaForm = new FormData(configForm);
  //console.log(configParaForm);
  let input = [], value = [];
  for(var pair of configParaForm.entries()) {
    //console.log(`input: ${pair[0]}, value: ${pair[1]}`);
    if (pair[1] != ''){
      input.push(pair[0]);
      value.push(pair[1]);
    }else if (pair[1] == '' && currentConfig[pair[0]]){
      delete currentConfig[pair[0]];
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
  console.log('saved');
  //console.dir(currentConfig);
};//end saveConfigParams

function modulesToCurrentConfig() {
  const mods = document.getElementById('module_ul').children;
  const selectedMods = [];
  for (var i = 0; i < mods.length; i++) {
    if (mods[i].classList.contains('modChoosen')) {
      selectedMods.push( mods[i].innerHTML);
    };
  };
  //const selectedMods = Array.from(document.getElementsByClassName('modChoosen'));
  if (selectedMods.length > 0) {
    currentConfig['modules'] = selectedMods;
  }else{
    delete currentConfig['modules'];
  };
  localStorage.setItem(currentConfig['project.configFile'].toString(), JSON.stringify(currentConfig));
};//end modulesToCurrentConfig

// NOTE: Something is broken here...
//Function for creating downloadable config file
// (function() {
//   makeTextFile = function() {
//     var text = "";
//     textFile = null;
//     //add modules to config first
//     if (currentConfig["modules"] != null){
//       for (var i = 0; i < currentConfig["modules"].length; i++) {
//         text += '#BioModule '.concat(currentConfig["modules"][i],"\n");
//       }
//     };
//     //for non_module
//     for (var key in currentConfig) {
//       console.log('making download');
//       console.log(key);
//       if (currentConfig.hasOwnProperty(key)) { //only lets keys that are user inputed pass
//         if (key == "modules" || key == "project.configFile") {// skipping project.configFile and modules
//         } else if (key.toString() != "project.configFile" || key != "modules") { //project.configFile doesn't go inside the document
//           text += key.concat("=", currentConfig[key], "\n");
//         }
//       }
//     }
//     var data = new Blob([text], {
//       type: 'text/plain'
//     });
//     // If we are replacing a previously generated file we need to manually revoke the object URL to avoid memory leaks.
//     if (textFile !== null) {
//       window.URL.revokeObjectURL(textFile);
//     }
//     textFile = window.URL.createObjectURL(data);
//     return textFile;
//   }//end makeTextFile
//
//   //gets all buttons with create class
//   var createDownload = document.getElementsByClassName('createDownload');
//   for (var i = 0; i < createDownload.length; i++) {
//     //console.log(createDownload[i]);
//     createDownload[i].addEventListener('click', function() {
//       //event.preventDefault()
//       var links = document.getElementsByClassName('downloadlink');
//       for (var a = 0; a < links.length; a++) {
//         let link = links[a];
//         console.log(links[a]);
//         console.log('links');
//         link['download'] = currentConfig["project.configFile"] + '.properties';
//         link.href = makeTextFile();
//         link.style.display = 'block';
//         };
//       }, false);
//     };//end forloop for createDownload
// })();

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
      if ( validateConfig() === true ){
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
      }
    } catch (e) {
      alert(e)
    }
  });//end eventlistener
};//end forloop

//for autosave
(function () {
  const configFormInputs = Array.from(document.getElementById('configForm').getElementsByTagName('input'));
  //const configTexts = configFormInputs.getElementsByTagName('text');
  const configTexts = configFormInputs.filter(inp => inp.type === 'text');
  //console.log(configTexts, 'text input');
  const configSelects = configFormInputs.filter(inp => inp.type === 'select');
  const configChecks = configFormInputs.filter(inp => inp.type === 'checkbox');;

  const configNumbers = configFormInputs.filter(inp => inp.type === 'number');;
  //console.log(configNumbers, 'num');

  for (let inp of configTexts){
  inp.addEventListener('change', saveConfigParamsForm, false);
  //console.log(inp);
  }

  for (let inp of configSelects){
  inp.addEventListener('change', saveConfigParamsForm, false)
  }

  for (let inp of configChecks){
  inp.addEventListener('click', saveConfigParamsForm, false);
  }

  for (let inp of configNumbers){
  inp.addEventListener('change', saveConfigParamsForm, false);
  //console.log(inp);
  }

  configFormInputs.forEach(inp => inp.onkeypress = function(e) {
  var key = e.charCode || e.keyCode || 0;
  if (key == 13) {
    saveConfigParamsForm();
    e.preventDefault();
    }
  })
})();
