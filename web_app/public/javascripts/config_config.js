/**
Author: Aaron Yerke
Purpose: Javascript for the portion of the web app that builds configuration files and runs them
Notes:
  This file contains most of the js related to the configuration object.
  config_menus.js contains the js for controlling the menu, which includes building the module selection
*/
function Config(modules = [], paramKeys = [], paramValues = [], comments = []){
  this.modules = modules;
  this.paramKeys = paramKeys;
  this.paramValues = paramValues;
  this.comments = comments;
  this.configPath;

  const _this = this; //hack for when some callback changes my 'this', used in this.saveConfigParamsForm

  this.loadLocal = async (event) => {//update Config from local object
    const file = event.target.files[0];
    try {
      //reset parameters back to empty
      this.modules = [], this.paramKeys = [], this.paramValues = [];

      //read in file and refill modules, keys, and values
      const fileContents = await readUploadedFileAsText(file);
      const results = readFlatFile(fileContents);
      this.modules = results[0];
      this.paramKeys = results[1];
      this.paramValues = results[2];
      this.paramKeys.push("pipeline.configFile");
      this.paramValues.push(file.name);
      console.dir(this);

      //set items in local storage
      //localStorage.setItem(file.name, JSON.stringify(this));

      saveConfigToGui({
        configName : file.name,
        configText : this.formatAsFlatFile(),
      });

      this.sendConfigDataToForms();
    } catch (e) {
      alert(e);
      console.error(e);
    }
  }//end load localhost

  this.sendConfigDataToForms = function(){
    console.log('in currentConfig.sendConfigDataToForms()');
    try {
      if (this.paramKeys.length != this.paramValues.length){
        alert('Your paramKeys should be the same length as your paramValues.  Find the error');
        return false;
      }
      //reorder module list elements to match the config
      myModules = orderModulesFromLocalFiles(this.modules.slice(), myModules);
      runModuleFunctions();

      //get all input elements for adding values too
      const selects = Array.from(document.getElementsByTagName('select'));
      const inputs = Array.from(document.getElementsByTagName('input'));
      const texts = inputs.filter(inp => inp.type === 'text');
      const radios = inputs.filter(inp => inp.type === 'radio');
      const checkboxs = inputs.filter(inp => inp.type === 'checkbox');
      const numbers = inputs.filter(inp => inp.type === 'number');

      //first step, loop through modules and show them
      for (mod of this.modules){
        var domModule = document.getElementById('module');
        var domModuleLi = domModule.getElementsByTagName('li');
        for (var b = 0; b < domModuleLi.length; b++) {//for mod in mod li
          if (mod == domModuleLi[b].getAttribute('data-link')) {
            try{
            domModuleLi[b].click();//add('modChoosen');
            }catch(err) {
              alert(err);
              console.error(err);
            }
          };//end if
        };//end for-loop over domModuleLi
      }//end this.modules for loop

      //Add parameter values to page inputs
      for (let i = 0; i < this.paramKeys.length; i++) {
        const key = this.paramKeys[i];
        const valueOfKey = this.paramValues[i];
        try {
          if (checkboxs.map(check => check.name).includes(key)){
            const checkTargets = checkboxs.filter(check => check.name == key);
            const cachedChecks = valueOfKey.split(',');
            cachedChecks.forEach(val => {
              checkTargets.find(check => check.value === val).checked = true;
            });
          }else{
            var targetInput = document.getElementById(key);
            if (targetInput.tagName == 'SELECT'){
              var select = selects.find(sel => sel.id == key);
              var opt =
              Array.apply(null, select.options).find(option => option.value === valueOfKey);
              if (opt.value != undefined){ opt.setAttribute('selected', true); };//select opt if not undefined
              }
            else if (targetInput.type == 'text' || targetInput.type == 'number'){
              targetInput.value = valueOfKey;
              }
            }//end else
          document.getElementById("mainMenu").style.display = "block";
        } catch (err) {
          alert(err + "\n problem with " + key + ".")
          console.error(err + "\n problem with " + key + ".");
        }
      }; //end for-loop over keys/parameters
    } catch (e) {
      console.error(e);
    }

  }//end this.sendConfigDataToForms

  this.saveConfigParamsForm = function (){
    _this.paramKeys = [], _this.paramValues = [];
    //let configForm = document.getElementById('configForm');
    let configFile = document.getElementById('pipeline.configFile');
    //console.log('configFile.value: ', configFile.value);
    if (configFile.value == ""){
      let now = new Date();
      let year = now.getYear() + 1900;
      let month = now.getMonth() + 1;
      let day = now.getDay();
      let c = 'Untitled_BLJ_project_'.concat(year,month,day);
      configFile.value = c;
    }
    //console.log('configFile.value: ', configFile.value);
    const configParaForm = new FormData(document.getElementById('configForm'));
    //console.log(configParaForm);
    for(var pair of configParaForm.entries()) {
      //console.log(`input: ${pair[0]}, value: ${pair[1]}`);
      if (pair[1] != ''){
        _this.paramKeys.push(pair[0]);
        //console.log(this);
        _this.paramValues.push(pair[1]);
        //console.log(_this.paramValues);
      };
    };
    _this.modulesToCurrentConfig();
    //var save = saveConfigToGui({test : "test"});
    if (this.configPath){
      console.log(this.configPath);
    }
    saveConfigToGui({
      configName : configFile.value,
      configText : _this.formatAsFlatFile(),
    });
    // localStorage.setItem(configFile.value, JSON.stringify(_this));

    console.log('saved');
    console.dir(_this);
  };//end this.saveConfigParams

  this.modulesToCurrentConfig = function() {
    const mods = document.getElementById('module_ul').children;
    this.modules = [];
    for (var i = 0; i < mods.length; i++) {
      if (mods[i].classList.contains('modChoosen')) {
        this.modules.push( mods[i].getAttribute('data-link'));
      };
    };
    console.log("this.paramKeys.indexOf('pipeline.configFile'): ", this.paramKeys.indexOf('pipeline.configFile'));
    saveConfigToGui({
        configName : this.paramValues[this.paramKeys.indexOf('pipeline.configFile')],
        configText : this.formatAsFlatFile(),
    });
    //localStorage.setItem(this.paramValues[this.paramKeys.indexOf('pipeline.configFile')], JSON.stringify(this));
  };//end modulesToCurrentConfig

  this.validateConfig = function() {
      const configForm = document.getElementById('configForm');
      const requiredParams = new Map(Object.entries({
        'input.dirPaths' : 'what is your sequence input directory path?',
        'report.taxonomyLevels' : 'What taxonomy levels would you like in the report?',
        'script.permissions' : 'what are the script permissions?',
        'script.defaultHeader' : 'what are the script default headers?',
        'demultiplexer.strategy' : 'What demultiplexing statagy do you want to use?',
        'pipeline.env' : 'In which enviroment do you wish to run this project?',
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
        'r_CalculateStats.pAdjustMethod' : 'Please choose your p-value adjust method.',
        'r_CalculateStats.pAdjustScope' : 'Please choose your p-value adjust scope.'
        }));
      const reqForEmail = new Map(Object.entries({
        'mail.encryptedPassword' : 'In order to receive an Emailed report, please provide your encrypted password.',
        'mail.from' : 'Please choose the Email address from which you want to recieve your Emailed report.',
        'mail.smtp.auth' : 'Please provide SMTP Authentication.',
        'mail.smtp.host' : 'Please provide your SMTP host.',
        'mail.to' : 'Please provide the Email recipient.',
        'mail.smtp.port' : 'Please provide the SMTP port.'
        }));

      const reqForMdsPlots = new Map(Object.entries({
        'r_PlotMds.numAxis' : 'Please provide the number of axes for the MDS plot.',
        'r_PlotMds.distance' : 'Please provide the number of axes for the MDS plot.',
        'r_PlotMds.outliers' : 'Please choose the Email address from which you want to recieve your Emailed report.',
        }));

      const reqForRdpClassifier = new Map(Object.entries({
        'rdp.minThresholdScore' : 'Please provide the RDP minium threshold score.'
        }));

      const reqForKrakenClassifier = new Map(Object.entries({
        'kraken.db' : 'Please provide a Kraken database.',
        }));

      const menuTabs = document.getElementsByClassName('tabcontent');
      const menuTabButtons = document.getElementsByClassName('tablinks');
      const moduleNameShortened = this.modules.forEach(mod => mod.slice('biolockj.module.'.length)); //broken

      let implicits = [],
        classifiers = [],
        implicitParsers = [],
        seqs = [],
        rs = [],
        reports = [];

      for (ele of myModules.entries()){
        //console.log(ele[1].catagory);
        switch (ele[1].category) {
          case 'implicit':
            implicits.push(ele[0].split('/').join('.'));
            break;
          case 'seq':
            seqs.push(ele[0].split('/').join('.'));
            break
          case 'classifier':
            classifiers.push(ele[0].split('/').join('.'));
            break;
          case 'implicit.parser':
            implicitParsers.push(ele[0].split('/').join('.'));
            break;
          case 'r':
            rs.push(ele[0].split('/').join('.'));
            break;
          case 'report':
            reports.push(ele[0].split('/').join('.'));
          default:
          //let all elements with no catagory fall through
        }
      }
      //console.log('reports: ',reports);

      //get parent div of any given node
      function getParentDiv(nodeId){
        let parentDiv;
        var node = document.getElementById(nodeId);
        if (node === null || node === undefined){
          alert(`We can't find ${nodeId}, perhaps we should add it to our parameters?`);
        }
        let counter = 4;
        while (parentDiv == undefined && counter > 0){
          console.log('getParentDiv node: ', node);
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

      //check for module dependencies based on the dependencyMap Map objects above
      function dependenciesPresent(dependencyMap){
        const menuTabsArray = Array.from(menuTabs);
        for (let para of dependencyMap.keys()){
          console.log(para);
          if (!(currentConfig.paramKeys.includes(para))){
            console.log(para);
            const currentMenuTab = getCurrentMenuTab();
            console.log(menuTabsArray.indexOf(currentMenuTab));
            currentMenuTab.style.display = 'none';
            menuTabButtons[menuTabsArray.indexOf(getParentDiv(para))].click();
            alert('Required information missing: '.concat(dependencyMap.get(para)))//change this to modal later
            highlightRequiredParam(para);
            return false;
          }//end if
        }//end for loop
      }
      if (this.paramKeys.length != this.paramValues.length){
        return false;
      }
      if (this.paramKeys.length < 1 ){
        return false;
      }

      //First check: check for required parameters
      if (dependenciesPresent(requiredParams) == false){
        return false;
      }

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
      if (this.modules.length === 0){
        alert('Please select at least one module.')
        return false;
      }
      for (let mod of this.modules){
        //check for dependencies
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
          if (!(this.paramKeys.contains('rarefier.max') || this.paramKeys.contains('rarefier.min'))){
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
          break;
        case 'classifier.wgs.KrakenClassifier':
          if (dependenciesPresent(reqForKrakenClassifier) == false){
            return false;
          }
          break;
        case 'classifier.wgs.SlimmClassifier':
          if (dependenciesPresent(reqForSlimmClassifier) == false){
            return false;
          }
          break;
        default: //optional
        console.log('All required parameters are found');
        }//end switch

        //check order
        const modsAfterMod = this.modules.slice(this.modules.indexOf(mod)+1);
        for (m of modsAfterMod){
          // If module = biolockj.module.classifier.*, no biolockj.module.seq.* modules can come after
          //console.log('m: ', m, ' mod: ', mod);
          //console.log('classifiers: ', classifiers);
          //console.log('seqs: ', seqs);
          if (classifiers.includes(mod) && seqs.includes(m)){
            alert('Please re-order your modules -no biolockj.module.seq modules can come after biolockj.module.classifiers.');
            return false;
          }
          // If module = biolockj.module.implicit.parser.*, no biolockj.module.seq.* or biolockj.module.classifier.* modules can come after
          if (implicitParsers.includes(mod)){
            if (seqs.includes(m) || classifiers.includes(m)){
              alert('Please re-order your modules -no biolockj.module.seq modules or biolockj.module.classifier modules can come after biolockj.module.implicit.parsers.');
              return false;
            }
          }
          // If module = biolockj.module.report, *no biolockj.module.seq.* or biolockj.module.classifier. *or biolockj.module.implicit.parser.* modules can come after
          if (reports.includes(mod)){
            if (seqs.includes(m) || classifiers.includes(m) || implicitParsers.includes(m)){
              alert('Please re-order your modules -no biolockj.module.seq modules, biolockj.module.implicit.parser modules, or biolockj.module.classifier modules can come after biolockj.module.implicit.parsers.');
              return false;
            }
          }
          // If module = biolockj.module.r. *no biolockj.module.seq.* or biolockj.module.classifier. *or biolockj.module.implicit.parser.* modules can come after
          if (rs.includes(mod)){
            if (seqs.includes(m) || classifiers.includes(m) || implicitParsers.includes(m)){
              alert('Please re-order your modules -no biolockj.module.seq modules, biolockj.module.implicit.parser modules, or biolockj.module.classifier modules can come after biolockj.module.implicit.parsers.');
              return false;
              }
            }
          }//end for (m of modsAfterMod){
        }//end for loop
      return true;
  };//end validation

  this.buildPartialLaunchArgument = function buildLaunchArgument(restart = false){
    /**
    Returns something like:
    inputDirPaths: "/Users/aaronyerke/git/blj_support/resources/test/data/multiplexed/combinedFastq"
    metadataFilePath: "/Users/aaronyerke/git/blj_support/resources/test/metadata/testMetadata.tsv"
    */
    const partialLaunchArgument = {};
    //config key : blj_argument
    //config Path will be built serverside
    const runtimeArguments = {
      'input.dirPaths' : 'i',
      'metadata.filePath' : 'm',
      // 'pipeline.configFile' : 'c',
      // TODO: Add -p to arguements list
    };
    for (var i = 0; i < this.paramKeys.length; i++) {
      if (Object.keys(runtimeArguments).includes(this.paramKeys[i])){
        partialLaunchArgument[runtimeArguments[this.paramKeys[i]]] = this.paramValues[i];
      }
    }
    console.log('partialLaunchArgument: ', partialLaunchArgument);
    return partialLaunchArgument;
  }

  this.formatAsFlatFile = function(){
    var text = "";
    textFile = null;
    try {
      //add modules to config first
      if ( this.modules != null && this.modules.length > 0 ){
        for (let i = 0; i < this.modules.length; i++) {
          text += '#BioModule '.concat(this.modules[i],"\n");;
        }
        //console.log(text);
      };
      //for non_module
      if ( this.paramKeys != null && this.paramKeys.length > 0 && this.paramValues != null && this.paramValues.length > 0 ){
        for (let i = 0; i < this.paramKeys.length; i++){
          text += this.paramKeys[i].concat("=", this.paramValues[i], "\n");
        }
      }
      return text;
    } catch (e) {
      console.log('formatAsFlatFile: ', e);
    };
  }//end formatAsFlatFile

  this.loadFromText = function(configText){
    this.modules = [], this.paramKeys = [], this.paramValues = [];
    const results = readFlatFile(configText);
    this.modules = results[0];
    this.paramKeys = results[1];
    this.paramValues = results[2];
  }

  function readFlatFile(fileString){
    const mods = [], pk = [], pv = [];
    //console.log('fileString: ',fileString);
    let lines = fileString.split('\n');
    for (let line = 0; line < lines.length; line++) {
      let lineSplit = lines[line].split("=");
      if (lines[line].slice(0, 11) === "#BioModule ") {
        mods.push(lines[line].slice(11));
      } else if (lineSplit.length >= 2 && !lines[line].startsWith('#')) { //if spliting at "=" is 2 or greater...
        pk.push(String(lineSplit[0]));
        pv.push(lineSplit.slice(1).join('='));
      } else if (line[lines] == undefined) {
      } else {
        alert('Lines must start with "#", or have key/values seperated by "=". Please check your config form');
        return
      };
    }//end forloop
    return [mods, pk, pv];
  }//end readFlatFile

  //function to convert file uploader to promise
  const readUploadedFileAsText = (inputFile) => {
    const temporaryFileReader = new FileReader();
    return new Promise((resolve, reject) => {
      temporaryFileReader.onerror = () => {
        temporaryFileReader.abort();
        reject(new DOMException("Problem parsing BioLockJ '.properties' file."));
      };
      temporaryFileReader.onload = () => {
        resolve(temporaryFileReader.result);
      };
      temporaryFileReader.readAsText(inputFile);
    });
  }//end readUploadedFileAsText

}//end Config prototype

var currentConfig = new Config();//IMPORTANT: This variable holds all of the selected configuations

//used for creating the table of of default config parameters. Used for getAllDefaultProps..
const defaultConfigs = []; //array to hold all of the configs.

document.getElementById('localFile').addEventListener('change', currentConfig.loadLocal);

//
function loadConfigPathToForm(conPath) {
  const tempConfig = retrievePropertiesFile({propertiesFile : conPath});
  tempConfig.then( propertiesFile => {
    console.log(propertiesFile);
    console.log("loadConfigPathToForm conPath:", conPath);
    currentConfig = new Config();
    currentConfig.loadFromText(propertiesFile.data);
    if (conPath.includes('/')){
      const split = conPath.split("/");
      currentConfig.paramKeys.push('pipeline.configFile');
      console.log(split[split.length - 1 ]);
      //take just the filename of the path and then take the .properties off
      //because we don't want the user to see the whole thing.  .properties
      //is added back on the save event.
      let configName = split[split.length - 1]
      if (configName.includes('.')){
        configName = configName.split('.')[0];

      }
      currentConfig.paramValues.push(configName);
    }else {
      currentConfig.paramKeys.push('pipeline.configFile');
      currentConfig.paramValues.push(conPath);
    }
    currentConfig.sendConfigDataToForms();
    currentConfig.configPath = conPath;
    console.log(currentConfig);
  })
}

//Adding all eventlisteners
//eventlistener for adding the recent config files to "recent"
document.getElementById("recent").addEventListener("mouseover", function() {
  const configs = retrieveConfigs();
  configs.then(retrievedConfigs => {
    console.log(retrievedConfigs);
    for (var i = 0; i < retrievedConfigs.length; i++) {
      let opt = document.createElement('a');
      opt.setAttribute("name", retrievedConfigs[i]);
      var text = document.createTextNode(retrievedConfigs[i].toString());
      opt.addEventListener("click", function() {
        loadConfigPathToForm('/config/' + this.name.concat('.properties'));
        })
      opt.appendChild(text);
      opt.classList.add('recentConfigs');
      let proj = document.getElementById("projects");
      proj.appendChild(opt);
    };
  })
}, {
 once: true
});

//eventlistener for adding the recent config files to "recent"
document.getElementById("restartListAnchor").addEventListener("mouseover", function() {
  const configs = retrievePipelines();
  configs.then(retrievedConfigs => {
    console.log('retrievedConfigs: ', retrievedConfigs);
    for (let i = 0; i < retrievedConfigs.names.length; i++) {
      console.log('retrievedConfigs.names[i]: ', retrievedConfigs.names[i]);
      let opt = document.createElement('a');
      let text = document.createTextNode(retrievedConfigs.names[i]);
      opt.addEventListener("click", function() {
        loadConfigPathToForm(retrievedConfigs.paths[i]);
        })
      opt.appendChild(text);
      opt.classList.add('recentConfigs');
      let proj = document.getElementById("restartListAnchor");
      proj.appendChild(opt);
      console.log('added opt', opt);
    };
  })
}, {
 once: true
});

const createDownload = document.getElementsByClassName('createDownload');
for (var i = 0; i < createDownload.length; i++) {
  createDownload[i].addEventListener('click', function() {
    event.preventDefault();
    currentConfig.saveConfigParamsForm();
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(currentConfig.formatAsFlatFile()));
    element['download'] = currentConfig.paramValues[currentConfig.paramKeys.indexOf("pipeline.configFile")];
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }, false);
};//end forloop for createDownload

for (const launch of document.getElementsByClassName("openLaunchModal")) {
  launch.addEventListener("click", function(event){
    event.preventDefault();
    // TODO: make a loop to add all forms to current config and do validation
    try {
      //tabForm.forEach( ele => ele.submit());
      currentConfig.saveConfigParamsForm();
      if ( currentConfig.validateConfig() === true ){
      launchModal.style.display = "block";

        const projectFolderNames = retrievePipelines();
        projectFolderNames.then((projNames) => {

          //erase old restart options
          const optionalProj = document.getElementById('projOptFieldSet');
          optionalProj.innerHTML = '';

          //add line
          let hr = document.createElement('hr');
          //l.innerHTML = 'Current Projects';
          optionalProj.appendChild(hr);

          //get all projects as options for restart
          for (var i = 0; i < projNames.length; i++) {
            let newRadio = document.createElement('input');
            newRadio.setAttribute('type', 'radio');
            newRadio.setAttribute('name', 'projLaunch');
            newRadio.setAttribute('value', projNames[i]);
            newRadio.setAttribute('name', 'projLaunch');
            newRadio.setAttribute('class', 'projLaunch');
            let newP = document.createElement('p');
            newP.innerHTML = `Use this configuration to restart ${projNames[i]}`;
            newP.appendChild(newRadio);
            optionalProj.appendChild(newP);
          }

          const projLaunchOptionsForm = document.getElementById('projectLaunchOptions');

          document.getElementById('launchBlj').addEventListener('click', function(){
            const projRadios = projLaunchOptionsForm.elements['projLaunch'];
            const launchAction = projLaunchOptionsForm.elements['restartOption']
            if (projRadios.value === 'launchNew'){
              launcher(projRadios.value);
            }else{
              launcher(launchAction.value, projRadios.value)
            }

          })
        });
      }
    } catch (e) {
      console.error(e);
    }
  });//end eventlistener
};//end forloop

//for autosave
const configFormInputs = Array.from(document.getElementById('configForm').getElementsByTagName('input'));
const configTexts = configFormInputs.filter(inp => inp.type === 'text');
const configSelects = Array.from(document.getElementById('configForm').getElementsByTagName('SELECT'));
const configChecks = configFormInputs.filter(inp => inp.type === 'checkbox');
const configNumbers = configFormInputs.filter(inp => inp.type === 'number');

for (let inp of configTexts){
inp.addEventListener('change', currentConfig.saveConfigParamsForm, false);
}

for (let inp of configSelects){
inp.addEventListener('change', currentConfig.saveConfigParamsForm, false)
}

for (let inp of configChecks){
inp.addEventListener('click', currentConfig.saveConfigParamsForm, false);
}

for (let inp of configNumbers){
inp.addEventListener('change', currentConfig.saveConfigParamsForm, false);
}

configFormInputs.forEach(inp => inp.onkeypress = function(e) {
var key = e.charCode || e.keyCode || 0;
if (key == 13) {
  currentConfig.saveConfigParamsForm();
  e.preventDefault();
  }
});

// Get the launch modal
const launchModal = document.getElementById('launchConsole');

// Get the <span> element that closes the modal
const launchSpan = Array.from(document.getElementsByClassName("closeModal"));

launchSpan.forEach(span => span.addEventListener('click', function(){
  this.parentNode.parentNode.style.display = "none";
}));

// When the user clicks on <span> (x), close the modal
// launchSpan.onclick = function() {
//     launchModal.style.display = "none";
// }


// NOTE: add event listener for default props section after talking to Mike
const dockerDefaultProps = document.getElementById('dockerDefaultProps');
dockerDefaultProps.addEventListener('click', function(){
  var defaultPath = '$BLJ/resources/config/default/docker.properties';
    currentConfig.paramKeys.push('pipeline.defaultProps');
    currentConfig.paramValues.push(defaultPath);
    getAllDefaultProps(defaultPath);
}, false);

document.getElementById('acceptDefaultProps').addEventListener('click', tableToCurrentConfig);
document.getElementById('seeResolvedDefaultProperties').addEventListener('click', function(){
  const parameterSelectorTable = document.getElementById('parameterSelectorTable');
  parameterSelectorTable.classList.remove('hidden');
  parameterSelectorTable.style.display = 'block';
})

function getAllDefaultProps(dfpath){

  const defaultFlatFile = retreiveDefaultProps(dfpath);

  defaultFlatFile.then( flatFile => {
    const defaultConfig = new Config();
    defaultConfig.loadFromText(flatFile);
    const splitPath = dfpath.split('/');


    //most config files won't have the file name as a property so we need to add it.
    defaultConfig.paramKeys.push('pipeline.configFile');
    console.log('splitPath[splitPath.length]: ', splitPath[splitPath.length-1]);
    defaultConfig.paramValues.push(splitPath[splitPath.length-1]);

    //now that our file has a name, add it to the default config array
    defaultConfigs.push(defaultConfig);
    console.log(defaultConfigs);

    console.log('defaultProps, ', defaultConfig.paramValues[defaultConfig.paramKeys.indexOf('pipeline.defaultProps')]);
    if (defaultConfig.paramValues[defaultConfig.paramKeys.indexOf('pipeline.defaultProps')] !== undefined){
      getAllDefaultProps(defaultConfig.paramValues[defaultConfig.paramKeys.indexOf('pipeline.defaultProps')])
    } else{
      console.log(defaultConfigs);
      //get the length of the longest array of paramKeys (mod of https://stackoverflow.com/questions/4020796/finding-the-max-value-of-an-attribute-in-an-array-of-objects)
      const tableLength = Math.max.apply(Math, defaultConfigs.map(function(config) { return config.paramKeys.length; }));
      let rowCounter = 0;
      const usedParamKeys = [];
      const table = document.getElementById("parameterSelectorTable");
      table.innerHTML = ''; //clear the table everytime

      // Create an empty <thead> element and add it to the table:
      var header = table.createTHead();

      // Create an empty <tr> element and add it to the first position of <thead>:
      var row = header.insertRow(0);
      const th = document.createElement('th');
      th.innerHTML = '';
      row.appendChild(th);
      //row.insertCell(0).innerHTML = ''

      //add the table headers
      for (var name = 0; name < defaultConfigs.length; name++) {
        // Insert a new cell (<td>) at the first position of the "new" <tr> element:
        const th = document.createElement('th');
        th.innerHTML = defaultConfigs[name].paramValues[defaultConfigs[name].paramKeys.indexOf('pipeline.configFile')];
        row.appendChild(th);
        //var cell = row.create(name+1);
        //cell.innerHTML = defaultConfigs[name].paramValues[defaultConfigs[name].paramKeys.indexOf('pipeline.configFile')];
      }//end for loop
      usedParamKeys.push('pipeline.configFile');
      rowCounter += 1;

      //cycle through the configs and add the parameters of each in order.
      for (var c = 0; c < defaultConfigs.length; c++) {
        const dfconfig = defaultConfigs[c];

        for (var pk = 0; pk < dfconfig.paramKeys.length-1; pk++) {
          if (!usedParamKeys.includes(dfconfig.paramKeys[pk])){
            const r = header.insertRow(-1);
            const th = document.createElement('th');
            th.innerHTML = dfconfig.paramKeys[pk];
            th.classList.add('rowHeader');
            r.appendChild(th);
            //r.insertCell(0).innerHTML = dfconfig.paramKeys[pk];

            for (var pv = 0; pv < defaultConfigs.length; pv++) {
              const cl = r.insertCell(pv+1);
              if (pv === c){
                cl.classList.add('modChoosen');
              }
              const param = defaultConfigs[pv].paramValues[defaultConfigs[pv].paramKeys.indexOf(dfconfig.paramKeys[pk])];
              if (param === undefined) {
                cl.innerHTML = '-';
                cl.classList.add('emptyTd');
              }else{
                cl.innerHTML = defaultConfigs[pv].paramValues[defaultConfigs[pv].paramKeys.indexOf(dfconfig.paramKeys[pk])];
              }
            }
            usedParamKeys.push(dfconfig.paramKeys[pk])
          }
        }
      }

      const defaultPropertiesModal = document.getElementById('defaultPropertiesModal');
      defaultPropertiesModal.style.display = "block";
    }
  })
}

function tableToCurrentConfig(){
  const table = document.getElementById('parameterSelectorTable')
  const paramKeys = [];
  const paramValues = [];

  for (let row of table.rows){
    for(let cell of row.cells){
      if (cell.classList.contains('rowHeader')){
        paramKeys.push(cell.innerText); // or cell.innerHtml (you can also set value to innerText/Html)
        console.log(paramKeys);
      }
      if (cell.classList.contains('modChoosen')){
        paramValues.push(cell.innerText); // or cell.innerHtml (you can also set value to innerText/Html)
      }
    }
  }//end for (let row of
  console.log('paramKeys len: ', paramKeys.length);
  currentConfig.paramKeys = paramKeys;
  currentConfig.paramValues = paramValues;
  //alert(currentConfig.paramKeys.length);
  currentConfig.sendConfigDataToForms();
  currentConfig.saveConfigParamsForm();
  table.classList.add('hidden');
}//function tableToCurrentConfig...

// retrieves chain of default props without user input, not used anymore
function resolveDefaultProps(config, docker = false){
  const dpropPath = config.paramValues[config.paramKeys.indexOf('pipeline.defaultProps')];
  console.log('dpropPath ', dpropPath);
  // if (!config.paramKeys.includes('pipeline.defaultProps')){
  //   console.error('no default props found');
  //   return;
  // }
  const defaultConfig = new Config();
  const dprop = retreiveDefaultProps(dpropPath, docker);
  dprop.then( retreived => {
    defaultConfig.loadFromText( retreived );
    console.log('defaultConfig ', defaultConfig);
    const defaultConfigDPath = defaultConfig.paramValues[defaultConfig.paramKeys.indexOf('pipeline.defaultProps')];
    console.log('dpropPat ',dpropPath);
    console.log('defaultConfigDPat ',defaultConfigDPath);
    if (defaultConfigDPath && defaultConfigDPath !== dpropPath){
      console.log('need to resolve again');
      return resolveDefaultProps(defaultConfig);
    }
    for (let i = 0; i < defaultConfig.paramKeys.length; i++) {
      const dfKey = defaultConfig.paramKeys[i]
      if (dfKey !== 'pipeline.configFile' && !config.paramKeys.includes(dfKey)){
        config.paramKeys.push(defaultConfig.paramKeys[i]);
        config.paramValues.push(defaultConfig.paramValues[i])
      }
    }
    return config;
  });
}

//returns config flat file
function retreiveDefaultProps(dpropPath) {
  return new Promise((resolve, reject) => {
    const request = new XMLHttpRequest();
    request.open('POST', '/defaultproperties', true);
    request.setRequestHeader("Content-Type", "application/json");
    request.send(JSON.stringify({
      file : dpropPath,
      //docker : docker,// can get rid of this
      //additional parameters for launch
    }));
    request.onreadystatechange = function() {
      if (request.readyState === XMLHttpRequest.DONE) {
        try{
          if(this.status === 200 && request.readyState === 4){
            resolve(this.responseText);
          }else{
            reject(this.status + " " + this.statusText)
          }
        } catch(e) {
          reject (e.message);
        }
      //window.location = '/progress';
      }//end request.onreadystatechange ...
    }// end if (docker === true){
  });
}//end retreiveDefaultProps

// When the user clicks anywhere outside of the modal, close it
// FIXME: commented out because it breaks .prjct hide
// window.onclick = function(event) {
//     if (event.target == launchModal) {
//         launchModal.style.display = "none";
//     }
// }

function sendFormToNode( formElementId, nodeAddress, requestMethod = 'POST') {
  return new Promise((resolve, reject) => {
    let formData = {};
    let myForm = new FormData(document.getElementById(formElementId).parentNode.parentNode);
    for (var i of myForm.entries()) {
      formData[i[0]] = i[1];
    }
    console.log('formData: ', formData);

    var request = new XMLHttpRequest();
    request.open(requestMethod, nodeAddress, true);
    request.setRequestHeader("Content-Type", "application/json");
    request.send(JSON.stringify({formData}));
    request.onreadystatechange = function() {
      if (request.readyState === XMLHttpRequest.DONE) {
        try {
          if(this.status === 200 && request.readyState === 4){
            console.log(this.responseText);
            resolve(this.responseText);
          }else{
            reject(this.status + " " + this.statusText)
          }
        } catch (e) {
          reject (e.message)
        }
      }
    }
  });
};

const projEnvInput = document.getElementById('pipeline.env');
['change','load', 'input'].forEach( evt =>
    projEnvInput.addEventListener(evt, function(event){
      event.preventDefault();
      const AwsButton = document.getElementById('AwsButton');
      console.log(`adding ${evt} listener to ${document.getElementById('pipeline.env')}`);
      switch (document.getElementById('pipeline.env').value) {
        case 'aws':
          AwsButton.classList.remove('hidden');
          AwsButton.click();
          break;
        default:
          AwsButton.classList.add('hidden');
      }
    }, false)
);

document.getElementById("manageConfigs").addEventListener("click", function() {
  console.log("manageConfigs clicked");
  updateConfigManager();
});

document.getElementById('submitDeleteConfig').addEventListener('click', event => {
  console.log(event);
  const datForm = new FormData(document.getElementById('configManagerForm'));
  datForm.forEach( input => {
    const request = new XMLHttpRequest();
    request.onreadystatechange = function() {
      if (request.readyState === XMLHttpRequest.DONE) {
        try {
          if (request.responseText === `deleted : ${input}`){
            const inputs = document.getElementsByName(input);
            inputs.forEach( delInput => {
              console.log(delInput);
              delInput.parentNode.remove();
            })
            updateConfigManager();
          }
        } catch (e) {
          console.error(e);
        }
      }
    }
    request.open('POST', '/deleteConfig', true);
    request.setRequestHeader("Content-Type", "application/json");
    request.send(JSON.stringify({ configFileName : input }));
  });//end forEach
})//end submitDeleteConfig').addEventListener(


if(typeof(EventSource) !== "undefined") {
	console.log('EventSource Works');
		// Yes! Server-sent events support!
    // Some code.....
	var StreamLog = new EventSource("/streamLog",{ withCredentials: true });
	StreamLog.addEventListener("message", function(e) {
	    console.log(e.data);
	}, false);

	StreamLog.addEventListener("open", function(e) {
	    console.log("StreamLog connection was opened.");
	}, false);

	StreamLog.addEventListener("error", function(e) {
	    console.log("Error - connection was lost.");
      StreamLog.close();//close on errors
	}, false);
	StreamLog.onmessage = function(event) {
		document.getElementById("log").innerHTML += event.data + "<br>";
	};
	//for getting progress from blj
	var streamProgress = new EventSource("/streamProgress",{ withCredentials: true });
	streamProgress.addEventListener("message", function(e) {
	    console.log(e.data);
	}, false);

	streamProgress.addEventListener("open", function(e) {
	    console.log("streamprogress connection was opened.");
	}, false);

	streamProgress.addEventListener("error", function(e) {
	    console.log("Error - connection was lost.");
	}, false);
	streamProgress.onmessage = function(event) {
		console.log('onmessage fired');
		console.log(event);
	//     //document.getElementById("result").innerHTML += event.data + "<br>";
	};
	var streamProgress = new EventSource("/streamProgress",{ withCredentials: true });
	streamProgress.addEventListener("message", function(e) {
	    console.log(e.data);
	}, false);

	streamProgress.addEventListener("open", function(e) {
	    console.log("streamprogress connection was opened.");
	}, false);

	streamProgress.addEventListener("error", function(e) {
	    console.log("Error - connection was lost.");
	}, false);
	streamProgress.onmessage = function(event) {
		console.log('onmessage fired');
		console.log(event);
	//     //document.getElementById("result").innerHTML += event.data + "<br>";
	};

} else {
	console.log('Sorry! No server-sent events support for this browser');
    // Sorry! No server-sent events support..
}
//Updates local list of configs based on list from node.
function updateConfigManager() {
  const manager = document.getElementById('configManager');
  const deleteDiv = document.getElementById('deleteConfigsDiv');
  const configs = retrieveConfigs();
  let br = document.createElement("br");
  deleteDiv.innerHTML = "";

  deleteDiv.innerHTML = "Please select the configuation files to delete: ";

  configs.then(configs => {
    for (var i = 0; i < configs.length; i++) {
      const checkbox = document.createElement('input');
      const deleteP = document.createElement('p');

      checkbox.setAttribute('type', 'checkbox');
      checkbox.setAttribute('value', configs[i]);
      checkbox.setAttribute('name', 'deleteConfigs');
      deleteP.innerHTML = configs[i];
      deleteP.appendChild(checkbox);
      deleteDiv.appendChild(deleteP)
      deleteDiv.appendChild(br);
      //managerForm.appendChild(p);
    }
    manager.style.display = "block";
  })
}

function launcher(launchAction = 'launchNew', restartProjectPath){
  event.preventDefault();
  //launchModal.style.display = "block";
  let requestParams = {
    modules : currentConfig.modules,
    paramKeys : currentConfig.paramKeys,
    paramValues : currentConfig.paramValues,
    partialLaunchArg : currentConfig.buildPartialLaunchArgument(),
    launchAction : launchAction,
    //additional parameters for launch
  }
  if (launchAction === 'restartFromCheckPoint' || launchAction === 'eraseThenRestart' ){
    requestParams['restartProjectPath'] = restartProjectPath;
    console.log(requestParams);
  }
  // else if (launchAction === 'eraseThenRestart') {
  //   requestParams['projectNameToDelete'] = projectNameToDelete;
  //   console.log(requestParams);
  // }

  var request = new XMLHttpRequest();
  request.open('POST', '/launch', true);
  request.setRequestHeader("Content-Type", "application/json");
  request.send(JSON.stringify(
    requestParams
  ));
  console.log('launch sent');
  request.onreadystatechange = function() {
  if (request.readyState == XMLHttpRequest.DONE) {
    console.log(request.responseText);
    //window.location = '/progress';
    }
  }
  console.log(request.responseText);
}

//delete config flat file
function deleteConfig(configFileName) {
  console.log(configFileName);
  const request = new XMLHttpRequest();
  request.onreadystatechange = function() {
    if (request.readyState === XMLHttpRequest.DONE) {
      try {
        console.log(request.responseText);
      } catch (e) {
        console.error(e);
      }
    }
  }
  request.open('POST', '/deleteConfig', true);
  request.setRequestHeader("Content-Type", "application/json");
  request.send(JSON.stringify({configFileName : configFileName}));
}//end deleteConfig(configFileName)

window.onload = function(){
  let configPath = document.getElementById('configPath').innerHTML.trim();
  if (configPath){
    console.log('Windo.onload configPath: ', configPath);
    document.getElementById('mainMenu').style.display = 'block';
    loadConfigPathToForm(configPath);
    currentConfig.configPath = configPath;
    // loadConfigPathToForm(configPath)
  }
}
