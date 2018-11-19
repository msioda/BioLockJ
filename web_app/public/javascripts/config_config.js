function Config(modules = [], paramKeys = [], paramValues = [], comments = []){
  this.modules = modules;
  this.paramKeys = paramKeys;
  this.paramValues = paramValues;
  this.comments = comments;
  const _this = this; //hack for when some callback changes my 'this', used in this.saveConfigParamsForm

  this.loadLocal = async (event) => {//update Config from local object
    const file = event.target.files[0];
    try {
      //reset parameters back to empty
      this.modules = [], this.paramKeys = [], this.paramValues = [];

      //read in file and refill modules, keys, and values
      const fileContents = await readUploadedFileAsText(file);
      let lines = fileContents.split('\n');
      for (let line = 0; line < lines.length; line++) {
        let lineSplit = lines[line].split("=");
        if (lines[line].slice(0, 11) === "#BioModule ") {
          this.modules.push(lines[line].slice(11));
        } else if (lineSplit.length >= 2 && !lines[line].startsWith('#')) { //if spliting at "=" is 2 or greater...
          this.paramKeys.push(String(lineSplit[0]));
          this.paramValues.push(lineSplit.slice(1).join('='));
        } else if (line[lines] == undefined) {
        } else {
          alert('Lines must start with "#", or have key/values seperated by "=". Please check your config form');
          return
        };
      }; //end for-loop
      this.paramKeys.push("project.configFile");
      this.paramValues.push(file.name);
      console.dir(this);

      //set items in local storage
      localStorage.setItem(file.name, JSON.stringify(this));

      //hide used file reader from user
      document.getElementById("openConfig").style.display = "none";

      this.sendConfigDataToForms();

    } catch (e) {
      alert(e);
    }
  }//end load localhost

  this.sendConfigDataToForms = function(){
    if (this.paramKeys.length != this.paramValues.length){
      alert('Your paramKeys should be the same length as your paramValues.  Find the error');
      return false;
    }
    //reorder module list elements to match the config
    myModules = orderModulesFromLocalFiles(this.modules, myModules);
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
        if (mod == domModuleLi[b].innerHTML) {
          try{
          domModuleLi[b].click();//add('modChoosen');
          }catch(err) {
            alert(err);
          }finally{
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
            //console.log(targetInput);
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
      } finally{
      }
    }; //end for-loop over keys/parameters
  }//end this.sendConfigDataToForms

  this.saveConfigParamsForm = function (){
    _this.paramKeys = [], _this.paramValues = [];
    //let configForm = document.getElementById('configForm');
    let configFile = document.getElementById('project.configFile');
    if (configFile.value == ""){
      let now = new Date();
      let year = now.getYear() + 1900;
      let month = now.getMonth() + 1;
      let day = now.getDay();
      let c = 'Untitled_BLJ_project_'.concat(year,'/',month,'/',day);
      configFile.value = c;
    }
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
    localStorage.setItem(configFile.value, JSON.stringify(this));
    console.log('saved');
    console.dir(_this);
    //console.dir(JSON.parse(localStorage.getItem(this.paramValues[paramKeys.indexOf('project.configFile')])));
  };//end this.saveConfigParams

  this.modulesToCurrentConfig = function() {
    const mods = document.getElementById('module_ul').children;
    this.modules = [];
    for (var i = 0; i < mods.length; i++) {
      if (mods[i].classList.contains('modChoosen')) {
        this.modules.push( mods[i].innerHTML);
      };
    };
    localStorage.setItem(this.paramValues[paramKeys.indexOf('project.configFile')], JSON.stringify(this));
  };//end modulesToCurrentConfig

  this.validateConfig = function() {
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
        'mail.smtp.auth' : 'Please provide SMTP Authentication.',
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

  this.buildPartialLaunchArgument = function buildLaunchArgument(){
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
    for (var i = 0; i < this.paramKeys.length; i++) {
      if (Object.keys(runtimeArguments).includes(this.paramKeys[i])){
        partialLauchArgument[runtimeArguments[this.paramKeys[i]]] = this.paramValues[i];
      }
    }
    return partialLauchArgument;
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
}//end Config prototype

var currentConfig = new Config();//IMPORTANT: This variable holds all of the selected configuations

//text to convert file uploader to promise
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
}
document.getElementById('localFile').addEventListener('change', currentConfig.loadLocal);

//Adding all eventlisteners
//eventlistener for adding the recent config files to "recent"
document.getElementById("recent").addEventListener("mouseover", function() {
  const recentMenuChoices = Object.keys(localStorage);
  for (var i = 0; i < recentMenuChoices.length; i++) {
    let opt = document.createElement('a');
    opt.setAttribute("name", recentMenuChoices[i]);
    var text = document.createTextNode(recentMenuChoices[i].toString());
    opt.addEventListener("click", function() {
     const tempConfig = JSON.parse(localStorage.getItem(this.name));
      console.log(tempConfig);
      currentConfig = new Config(tempConfig.modules, tempConfig.paramKeys, tempConfig.paramValues);
      currentConfig.sendConfigDataToForms();
      console.log(currentConfig);
    });
    opt.appendChild(text);
    opt.setAttribute('position', 'relative');
    opt.setAttribute('display', 'block');
    opt.classList.add('recentConfigs');
    let proj = document.getElementById("projects");
    proj.appendChild(opt);
  };
}, {
 once: true
});

const createDownload = document.getElementsByClassName('createDownload');
for (var i = 0; i < createDownload.length; i++) {
  createDownload[i].addEventListener('click', function() {
    //event.preventDefault();
    const element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(currentConfig.formatAsFlatFile()));
    element['download'] = currentConfig.paramValues[currentConfig.paramKeys.indexOf("project.configFile")];
    element.style.display = 'none';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }, false);
};//end forloop for createDownload

for (const launch of document.getElementsByClassName("launchBlj")) {
  launch.addEventListener("click", function(event){
    event.preventDefault();
    // TODO: make a loop to add all forms to current config and do validation
    try {
      //tabForm.forEach( ele => ele.submit());
      currentConfig.saveConfigParamsForm();
      if ( currentConfig.validateConfig() === true ){
        var request = new XMLHttpRequest();
        request.open('POST', '/launch', true);
        request.setRequestHeader("Content-Type", "application/json");
        request.send(JSON.stringify({
          modules : currentConfig.modules,
          paramKeys : currentConfig.paramKeys,
          paramValues : currentConfig.paramValues,
          partialLaunchArg : currentConfig.buildPartialLaunchArgument(),
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
  const configFormInputs = Array.from(document.getElementById('configForm').getElementsByTagName('input'));
  //const configTexts = configFormInputs.getElementsByTagName('text');
  const configTexts = configFormInputs.filter(inp => inp.type === 'text');
  //console.log(configTexts, 'text input');
  const configSelects = configFormInputs.filter(inp => inp.type === 'select');
  const configChecks = configFormInputs.filter(inp => inp.type === 'checkbox');;

  const configNumbers = configFormInputs.filter(inp => inp.type === 'number');;
  //console.log(configNumbers, 'num');

  for (let inp of configTexts){
  inp.addEventListener('change', currentConfig.saveConfigParamsForm, false);
  //console.log(inp);
  }

  for (let inp of configSelects){
  inp.addEventListener('change', currentConfig.saveConfigParamsForm, false)
  }

  for (let inp of configChecks){
  inp.addEventListener('click', currentConfig.saveConfigParamsForm, false);
  }

  for (let inp of configNumbers){
  inp.addEventListener('change', currentConfig.saveConfigParamsForm, false);
  //console.log(inp);
  }

  configFormInputs.forEach(inp => inp.onkeypress = function(e) {
  var key = e.charCode || e.keyCode || 0;
  if (key == 13) {
    currentConfig.saveConfigParamsForm();
    e.preventDefault();
    }
  });
