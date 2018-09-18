var currentConfig = {};//IMPORTANT: This variable holds all of the selected configuations
function configValidation(config){
  let formNames = [];
  config.keys(obj).forEach(function(key,index) {
    let keyForm = key.split('.')
    console.log(keyForm);
    if (!formNames.contains(keyForm)){
      formNames.push(keyForm);
    }
  });//end forEach
}//end configValidation
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
class configO {
  constructor() {
    this.params = params;
    this.methods = methods;
  }
}

/* When the user clicks on the "Projects"button,
            toggle between hiding and showing the dropdown content */
function projectOptions() {
  document.getElementById("projects").classList.toggle("show");
}

//Open the main Menu when "Projects" -> "New" is clicked
function newProj() {
  document.getElementById("mainMenu").style.display = "block";
  //reset the forms
  var forms = document.getElementsByTagName('form');
  for (var i = 1; i < forms.length; i++) { //skips first empty form
    forms[i].reset();
  }
  var selects = document.getElementsByTagName('select')
  for (var s = 0; s < selects.length; s++){
    selects[s].value = '';
  }
}

// Close the dropdown if the user clicks outside of it
window.onclick = function(e) {
  if (!e.target.matches('.dropbtn')) {
    var myDropdown = document.getElementById("projects");
    if (myDropdown.classList.contains('show')) {
      myDropdown.classList.remove('show');
    }
  }
}

function loadLocalFile() {//loading from computer
  var file = document.getElementById("localFile").files[0];
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
          //console.log(lines[line]);
          provisionalConfigObject.modules.push(lines[line]);
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
        sendConfigDataToForms(provisionalConfigObject);
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
var sendConfigDataToForms = function(configObject) {
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
            if (mods[a] == '#BioModule ' + domModuleLi[b].innerHTML) {
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

//function for clicking tabs
function openTab(evt, tabName) {
  // Declare all variables
  var i, tabcontent, tablinks;

  // Get all elements with class="tabcontent" and hide them
  tabcontent = document.getElementsByClassName("tabcontent");
  for (i = 0; i < tabcontent.length; i++) {
    tabcontent[i].style.display = "none";
  }

  // Get all elements with class="tablinks" and remove the class "active"
  tablinks = document.getElementsByClassName("tablinks");
  for (i = 0; i < tablinks.length; i++) {
    tablinks[i].className = tablinks[i].className.replace("active", "");
  }

  // Show the current tab, and add an "active" class to the button that opened the tab
  document.getElementById(tabName).style.display = "block";
  evt.currentTarget.className += " active";
}
// Get the element with id="defaultOpen" and click on it (making it open by default)
document.getElementById("defaultOpen").click();

//make "open" show open div
function openProjDisplay() {
  // Check for the various File API support.
  if (window.File && window.FileReader && window.FileList && window.Blob) {
    // Great success! All the File APIs are supported.
    document.getElementById("openConfig").style.display = "block";
  } else {
    alert('The File APIs are not fully supported in this browser.');
  }
};//end openProjDisplay

function saveConfigParamsForm(event){
  event.preventDefault()
  const configParaForm = new FormData(event.target);
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
  localStorage.setItem(currentConfig['project.configFile'].toString(), JSON.stringify(currentConfig));
  console.dir(currentConfig);
  modulesToCurrentConfig();
};//end saveConfigParams

const tabForm = document.getElementsByClassName('tabForm');
for (const ele of tabForm)
{ele.addEventListener('submit', event => saveConfigParamsForm(event))};

function finalSubmit(){
  tabForm.forEach(ele => ele.submit());
}

//eventlistener for adding the recent config files to "recent"
document.getElementById("recent").addEventListener("mouseover", function() {
  recentMenuChoices = Object.keys(localStorage);
  for (var i = 0; i < recentMenuChoices.length; i++) {
    let opt = document.createElement('a');
    opt.setAttribute("name", recentMenuChoices[i]);
    var text = document.createTextNode(recentMenuChoices[i].toString());
    opt.addEventListener("click", function() {
      sendConfigDataToForms(JSON.parse(localStorage.getItem(this.name)));
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

/**Helper code to grab text from github for parsing module descriptions from
-className will be value like biolockj.module.seq.AwkFastaConverter
-bljModuleJavaClassName=”biolockj/module/seq/AwkFastaConverter.java”*/
function getUrl(bljModuleJavaClassName){
  //This function makes the string to feed into getText
  console.assert(typeof bljModuleJavaClassName === "string", "getUrl function requires string argumentd")
  urlRoot="https://raw.githubusercontent.com/msioda/BioLockJ/master/src/"
  return (urlRoot+bljModuleJavaClassName)
}//end getUrl
function getText(bljLink){// read text from URL location
  return new Promise((resolve, reject) => {
    var request = new XMLHttpRequest();
    request.open('GET', bljLink, true);
    request.onload = function() {
      try{
        if(this.status === 200 && request.readyState === 4){
          resolve(this.responseText)
        }else{
          reject(this.status + " " + this.statusText)
        }
      } catch(e) {
        reject (e.message);
      }
    };
    request.onerror = function() {
      reject(this.status + " " + this.statusText)
    };
    request.send();
  })
}//end getText

function parseBljModuleJavaClass(text){
  var startPublicClass;
  var startComment;
  var lines = text.split('\n');
  for (let i = 0; i < lines.length; i++){
    if (lines[i].startsWith("public class")){
      startPublicClass = i;
    }
  }
  for (let i = startPublicClass; i > 0; i--) {
    if (lines[i].startsWith("/**")) {
      startComment = i;
    }
  }
  var modDescrip = ""
  for (var a = startComment; a < startPublicClass; a++){
    var sect = lines[a].slice(3,);
    modDescrip = modDescrip.concat(sect);
  }
  return(modDescrip)
}//end parseBljModuleJavaClass

//section for module related functions
function modulesToCurrentConfig() {
  mods = document.getElementById('module').getElementsByTagName('li');
  selectedMods = [];
  for (var i = 0; i < mods.length; i++) {
    if (mods[i].classList.contains('modChoosen') && mods[i].disabled != true) {
      selectedMods.push("#BioModule " + mods[i].innerHTML)
    };
  };
  if (selectedMods.length > 0) {
    currentConfig['modules'] = selectedMods;
  }else{delete currentConfig['modules']}
  localStorage.setItem(currentConfig['project.configFile'].toString(), JSON.stringify(currentConfig));
};

/*This function object will be used to keep track of how many of each moduleClass is chosen
modsToDisable will be one of the _____ModChoosen*/
function moduleCounter(modulesToDisable) {
  this.count = 0;
  this.modsToDisable = modulesToDisable;
  this.getCount = function(){
    return this.count
  };
  this.decrementCount = function(){
    this.count--
  };
  this.incrementCount = function(){
    this.count++
  };
  this.modClassSelected = function(target){
  if (target.classList.contains("modChoosen")) {
    target.classList.remove("modChoosen");
      this.decrementCount();
      if (this.getCount() == 0) {
        for (let t = 0; t < this.modsToDisable.length; t++) {
          removeClassToAllElemInList(this.modsToDisable[t], "disabledMod");
        }
      }
  }else {
    target.classList.add("modChoosen");
      this.incrementCount();
      if (this.getCount() > 0) {
        for (let t = 0; t < this.modsToDisable.length; t++) {
          addClassToAllElemInList(this.modsToDisable[t], "disabledMod");
        }
      }
    }//end else
  }//end modClassSelected
};//end moduleCounter

//helper function for modTrackers
function addClassToAllElemInList(objects, className) {
  for (let r = 0; r < objects.length; r++) {
    objects[r].classList.add(className);
  }
};
//helper function for modTrackers
function removeClassToAllElemInList(objects, className) {
  for (let r = 0; r < objects.length; r++) {
    objects[r].classList.remove(className);
  }
};

function hoverEventlistenerForModules(modLiElement){
  modLiElement.addEventListener("mouseover", function() {
  let infoTarget = document.getElementById("moduleInfoDiv");
  let info = '<!DOCTYPE html><html><head></head><body><div id="tempModInfo">' + this.getAttribute('data-info').trim() + '</div></body></html>';
  try {
    parser = new DOMParser;
    newInfo = parser.parseFromString(info , "text/xml");
    while (infoTarget.firstChild) {
      infoTarget.removeChild(infoTarget.firstChild);
      }
    try {
      infoTarget.appendChild(newInfo.getElementById('tempModInfo'));
      if (document.getElementById('tempModInfo').innerHTML == ""){
        document.getElementById('tempModInfo').insertAdjacentHTML("afterbegin", this.getAttribute('data-info'));
      }
  } catch (e) {
    console.error(e);
  } finally {
      //do something?
    }
  }
  catch (err){
      console.error(err);
  }
  })//end eventlistener
}//end event listener wrapper function

//module drag events
/* comes from http://syntaxxx.com/rearranging-web-page-items-with-html5-drag-and-drop/*/
function dragStarted(evt) {
  //start drag
  console.log('drag started');
  console.log(evt);
  this.source = evt.target;
  //set data
  data = {
    html: evt.target.innerHTML,
    classes: evt.target.classList.value,
    info: evt.target.info,
    value: evt.target.info,
  };
  evt.dataTransfer.setData("text/plain", JSON.stringify(data));
  //specify allowed transfer
  evt.dataTransfer.effectAllowed = "move";
};

function draggingOver(evt) {
  //drag over
  evt.preventDefault();
  //specify operation
  evt.dataTransfer.dropEffect = "move";
};

function dropped(evt) {//function for dropping dragged modules
  evt.preventDefault();
  evt.stopPropagation();
  //update text in dragged item
  this.source.innerHTML = evt.target.innerHTML;
  this.source.classList = evt.target.classList;
  this.source.info = evt.target.info;
  //update text in drop target
  data = JSON.parse(evt.dataTransfer.getData("text/plain"));
  console.log(data);
  evt.target.innerHTML = data.html;
  evt.target.classList = data.classes;
  evt.target.info = data.info;
}

/* the following block adds an eventlistener with two functions to the next
tab tabButtons one for saving, and one for jumping to the next tab */
var tabButtons = Array.from(document.getElementsByClassName("nextTab"));
for (var t = 0; t < tabButtons.length; t++) {
  var tabs = Array.from(document.getElementsByClassName("tabcontent"));
  //eventlistener for moving to next tabName
  tabButtons[t].tabNum = t;
  tabButtons[t].addEventListener("click", function() {
      saveTab(this.parentNode.id);
      openTab(event, tabs[this.tabNum + 1].getAttribute("id"));
    }, {
      once: false
    } //end of eventlistener
  ) //end of nextTab eventlistener
}; //end tabs for-loop

//list of modules with their java classes and their javascript classes
//has the format of nested array, with the first item of the nested array being the java class
const moduleLinkAndClass = [
    ['biolockj/module/implicit/ImportMetadata.java', 'implicit', 'hidden'],
    ['biolockj/module/implicit/Demultiplexer.java', 'implicit', 'hidden'],
    ['biolockj/module/classifier/r16s/QiimeClosedRefClassifier.java', 'qiimeClass'],
    ['biolockj/module/classifier/r16s/RdpClassifier.java', 'rdpClass'],
    ['biolockj/module/implicit/parser/r16s/RdpParser.java', 'rdpClass', 'implicit', 'hidden'],
    ['biolockj/module/classifier/r16s/QiimeDeNovoClassifier.java', 'qiimeClass'],
    ['biolockj/module/classifier/r16s/QiimeOpenRefClassifier.java', 'qiimeClass'],
    ['biolockj/module/classifier/wgs/KrakenClassifier.java', 'krakenClass'],
    ['biolockj/module/classifier/wgs/MetaphlanClassifier.java',  'metaphlanClass'],
    ['biolockj/module/classifier/wgs/SlimmClassifier.java', 'slimmClass'],
    ['biolockj/module/r/BuildMdsPlots.java'],
    ['biolockj/module/r/BuildOtuPlots.java'],
    ['biolockj/module/r/BuildPvalHistograms.java'],
    ['biolockj/module/r/CalculateStats.java'],
    ['biolockj/module/report/AddMetadataToOtuTables.java'],
    ['biolockj/module/report/JsonReport.java'],
    ['biolockj/module/report/Normalizer.java'],
    ['biolockj/module/seq/AwkFastaConverter.java'],
    ['biolockj/module/seq/Multiplexer.java'],
    ['biolockj/module/seq/PearMergeReads.java'],
    ['biolockj/module/seq/Gunzipper.java'],
    ['biolockj/module/seq/Rarefier.java'],
    ['biolockj/module/implicit/RegisterNumReads.java','implicit', 'hidden'],
    ['biolockj/module/seq/SeqFileValidator.java'],
    ['biolockj/module/seq/TrimPrimers.java'],
    ['biolockj/module/report/Email.java']
];

function runModuleFunctions() {//large function to build module li and counters
  function toggleSelectModule(target) {//function called when modules are selected, it both selects them and disables others
    if (target.classList.contains("qiimeClass")) {
      qiimeModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("rdpClass")) {
      rdpModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("krakenClass")) {
      krakenModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("slimmClass")) {
      slimmModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("metaphlanClass")) {
      metaphlanModuleCounter.modClassSelected(target)
    }else{
      //for all modules that are not a classifier
      if (target.classList.contains("modChoosen")) {
      target.classList.remove("modChoosen");
      }else{
        target.classList.add("modChoosen");
        }
    }
  };// end toggleSelectModule

  function makeModuleLi(link, classes){
    var modUl = document.getElementById('module_ul');
    var mod = document.createElement('li');
    for (var c = 0; c < classes.length; c++){
      mod.classList.add(classes[c])
    }//  ^this function is a hack because classList.add() adds commas between the css classes, making them unreadable to css
    mod.setAttribute('draggable', true);
    mod.innerHTML = link.split('.')[0].replace(/\//g,'.');//remove .java then replace / with .
    var text = getText(getUrl(link))
    text.then(result => {
      mod.setAttribute('data-info', parseBljModuleJavaClass(result));
      hoverEventlistenerForModules(mod);
    });
    mod.addEventListener('dragstart', function(){dragStarted(event)});
    mod.addEventListener('dragover', function(){draggingOver(event)});
    mod.addEventListener('drop',function(){dropped(event)});
    mod.addEventListener('click', function(){toggleSelectModule(event.target)})
    modUl.appendChild(mod);
  };//end makeModuleLi

  for (var i = 0; i < moduleLinkAndClass.length; i++){
    mod = moduleLinkAndClass[i];
    try {
      makeModuleLi(mod[0], mod.slice(1,));
    } catch (e) {
      console.error(e);
    } finally {
    }
  }//end forloop

  //the following are list of module nodes with class for building the subsequence "Choosen" lists
  var qiimeClassModNodes = Array.from(document.getElementsByClassName("qiimeClass"));
  var slimmClassModNodes = Array.from(document.getElementsByClassName("slimmClass"));
  var krakenClassModNodes = Array.from(document.getElementsByClassName("krakenClass"));
  var rdpClassModNodes = Array.from(document.getElementsByClassName("rdpClass"));
  var metaphlanClassModNodes = Array.from(document.getElementsByClassName("metaphlanClass"));

  //moduleCounters instanciated with nodes that they will disable
  var qiimeModuleCounter = new moduleCounter([slimmClassModNodes, krakenClassModNodes, rdpClassModNodes, metaphlanClassModNodes]);
  var rdpModuleCounter = new moduleCounter([slimmClassModNodes, krakenClassModNodes, qiimeClassModNodes, metaphlanClassModNodes]);
  var krakenModuleCounter = new moduleCounter([slimmClassModNodes, qiimeClassModNodes, rdpClassModNodes, metaphlanClassModNodes]);
  var slimmModuleCounter = new moduleCounter([qiimeClassModNodes, krakenClassModNodes, rdpClassModNodes, metaphlanClassModNodes]);
  var metaphlanModuleCounter = new moduleCounter([slimmClassModNodes, krakenClassModNodes, rdpClassModNodes, qiimeClassModNodes]);

  document.getElementById('project.allowImplicitModules').addEventListener('change', function(evt){
    const implicits = Array.from(document.getElementsByClassName('implicit'));
    if (this.value === 'Y'){ implicits.forEach(imp => {imp.classList.remove('hidden')});
    }else{implicits.forEach(imp => imp.classList.add('hidden'));}
  });
};//end runModuleFunctions
runModuleFunctions();

for (const launch of document.getElementsByClassName("launchBlj")) {
  launch.addEventListener("click", function(){
    // TODO: make a loop to add all forms to current config and do validation
    try {
      //tabForm.forEach( ele => ele.submit());

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
        //window.location = '/progress';
        }
      }
      console.log(request.responseText);
    } catch (e) {
      alert(e)
    }

  });//end eventlistener
};//end forloop

//Function for creating downloadable config file
(function() {
  makeTextFile = function() {
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
