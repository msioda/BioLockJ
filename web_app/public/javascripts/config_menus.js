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

/**Helper code to grab text from github for parsing module descriptions from
-className will be value like biolockj.module.seq.AwkFastaConverter
-bljModuleJavaClassName=”biolockj/module/seq/AwkFastaConverter.java”*/
function getUrl(bljModuleJavaClassName){
  //This function makes the string to feed into getText
  console.assert(typeof bljModuleJavaClassName === "string", "getUrl function requires string argumentd")
  urlRoot="https://raw.githubusercontent.com/mikesioda/BioLockJ_Dev/master/src/"
  return (urlRoot + bljModuleJavaClassName + '.java')
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

function parseBljModuleJavaClass(text){//gets text java documentation from Java class
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
  return modDescrip;
}//end parseBljModuleJavaClass

//section for module related functions
// function modulesToCurrentConfig() {
//   mods = document.getElementById('module').getElementsByTagName('li');
//   selectedMods = [];
//   for (var i = 0; i < mods.length; i++) {
//     if (mods[i].classList.contains('modChoosen') && mods[i].disabled != true) {
//       selectedMods.push( mods[i].innerHTML);
//     };
//   };
//   if (selectedMods.length > 0) {
//     currentConfig['modules'] = selectedMods;
//   }else{delete currentConfig['modules']}
//   localStorage.setItem(currentConfig['project.configFile'].toString(), JSON.stringify(currentConfig));
// };

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
  saveConfigParamsForm();
}
// NOTE: I think this block is no longer in use
// /* the following block of code adds an eventlistener with two functions to the next
// tab tabButtons one for saving, and one for jumping to the next tab */
// var tabButtons = Array.from(document.getElementsByClassName("nextTab"));
// for (var t = 0; t < tabButtons.length; t++) {
//   var tabs = Array.from(document.getElementsByClassName("tabcontent"));
//   //eventlistener for moving to next tabName
//   tabButtons[t].tabNum = t;
//   tabButtons[t].addEventListener("click", function() {
//       //saveTab(this.parentNode.id);
//       //openTab(event, tabs[this.tabNum + 1].getAttribute("id"));
//     }, {
//       once: false
//     } //end of eventlistener
//   ) //end of nextTab eventlistener
// }; //end tabs for-loop

//list of modules with their java classes and their javascript classes
//has the format of nested array, with the first item of the nested array being the java class
// const moduleLinkAndClass = [
//     ['biolockj/module/implicit/ImportMetadata.java', 'implicit', 'hidden'],
//     ['biolockj/module/implicit/Demultiplexer.java', 'implicit', 'hidden'],
//     ['biolockj/module/seq/SeqFileValidator.java'],
//     ['biolockj/module/seq/TrimPrimers.java'],
//     ['biolockj/module/seq/Rarefier.java'],
//     ['biolockj/module/classifier/r16s/QiimeClosedRefClassifier.java', 'qiimeClass'],
//     ['biolockj/module/classifier/r16s/RdpClassifier.java', 'rdpClass'],
//     ['biolockj/module/implicit/parser/r16s/RdpParser.java', 'rdpClass', 'implicit', 'hidden'],
//     ['biolockj/module/classifier/r16s/QiimeDeNovoClassifier.java', 'qiimeClass'],
//     ['biolockj/module/classifier/r16s/QiimeOpenRefClassifier.java', 'qiimeClass'],
//     ['biolockj/module/classifier/wgs/KrakenClassifier.java', 'krakenClass'],
//     ['biolockj/module/classifier/wgs/MetaphlanClassifier.java',  'metaphlanClass'],
//     ['biolockj/module/classifier/wgs/SlimmClassifier.java', 'slimmClass'],
//     ['biolockj/module/report/Normalizer.java'],
//     ['biolockj/module/r/BuildMdsPlots.java'],
//     ['biolockj/module/r/BuildOtuPlots.java'],
//     ['biolockj/module/r/BuildPvalHistograms.java'],
//     ['biolockj/module/r/CalculateStats.java'],
//     ['biolockj/module/report/AddMetadataToOtuTables.java'],
//     ['biolockj/module/report/JsonReport.java'],
//     ['biolockj/module/seq/AwkFastaConverter.java'],
//     ['biolockj/module/seq/Multiplexer.java'],
//     ['biolockj/module/seq/PearMergeReads.java'],
//     ['biolockj/module/seq/Gunzipper.java'],
//     ['biolockj/module/implicit/RegisterNumReads.java','implicit', 'hidden'],
//     ['biolockj/module/report/Email.java']
// ];

//Creating a Map object for module information
myModules = new Map(Object.entries({
  'biolockj/module/implicit/ImportMetadata' :
  { cssClass : ['implicit','hidden'], label : 'Metadata Importer'},

  'biolockj/module/implicit/Demultiplexer' : { cssClass : ['implicit', 'hidden']},

  'biolockj/module/seq/SeqFileValidator' : { cssClass : []},

  'biolockj/module/seq/TrimPrimers' : { cssClass : []},

  'biolockj/module/seq/Rarefier' : { cssClass : []},

  'biolockj/module/classifier/r16s/QiimeClosedRefClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter'},

  'biolockj/module/classifier/r16s/RdpClassifier' :
  { cssClass : ['rdpClass', 'classifierUnique'], label : "16S Classifier: RDP", counter : 'rdpModuleCounter' },

  'biolockj/module/implicit/parser/r16s/RdpParser' :
  { cssClass : ['rdpClass', , 'classifierUnique', 'implicit', 'hidden'], counter : 'rdpModuleCounter' },

  'biolockj/module/classifier/r16s/QiimeDeNovoClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter'},

  'biolockj/module/classifier/r16s/QiimeOpenRefClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter'},

  'biolockj/module/classifier/wgs/KrakenClassifier' :
  { cssClass : ['krakenClass', 'classifierUnique'], counter : 'krakenModuleCounter'},

  'biolockj/module/classifier/wgs/MetaphlanClassifier' :
  { cssClass : ['metaphlanClass', 'classifierUnique'], counter : 'metaphlanModuleCounter'},

  'biolockj/module/classifier/wgs/SlimmClassifier' :
  { cssClass : ['slimmClass', 'classifierUnique'], counter : 'slimmModuleCounter'},

  'biolockj/module/report/Normalizer' : { cssClass : []},

  'biolockj/module/r/BuildMdsPlots' : { cssClass : []},

  'biolockj/module/r/BuildOtuPlots' : { cssClass : []},

  'biolockj/module/r/BuildPvalHistograms' : { cssClass : []},

  'biolockj/module/r/CalculateStats' : { cssClass : []},

  'biolockj/module/report/AddMetadataToOtuTables' : { cssClass : []},

  'biolockj/module/report/JsonReport' : { cssClass : []},

  'biolockj/module/seq/AwkFastaConverter' : { cssClass : []},

  'biolockj/module/seq/Multiplexer' : { cssClass : []},

  'biolockj/module/seq/PearMergeReads' : { cssClass : []},

  'biolockj/module/seq/Gunzipper' : { cssClass : []},

  'biolockj/module/implicit/RegisterNumReads' : { cssClass : ['implicit', 'hidden']},

  'biolockj/module/report/Email' : { cssClass : []},

}));

//reorder moduleLinkAndClass to suit config form
function orderModulesFromLocalFiles(selectedModulesArray, defaultOrderMap){

  if (selectedModulesArray == undefined){
    return defaultOrderMap;
  }

  for (let mod  of selectedModulesArray){
    if (Array.from(defaultOrderMap.keys()).includes( mod )){
    alert('Unknown modules in local file: ' + mod);
    return false;
    }
  }

  const selectedModulesArraySet = new Set(selectedModulesArray);

  if (selectedModulesArray.length > selectedModulesArraySet.size){
    alert("There are duplicate modules in your local file.")
    return false;
  }

  let reorderedMods = new Map()//

  while (defaultOrderMap.size > 0){
    for (let mod of defaultOrderMap.keys()){

      if (!selectedModulesArray.includes(mod)){
        reorderedMods.set(mod, defaultOrderMap.get(mod));
        defaultOrderMap.delete(mod);
      }else if (mod == selectedModulesArray[0]){
        reorderedMods.set(mod, defaultOrderMap.get(mod));
        defaultOrderMap.delete(mod);
        selectedModulesArray.shift();
      }
    }//end for
  }//end while

  return reorderedMods;
}
//let test = orderModulesFromLocalFiles(['biolockj/module/r/BuildPvalHistograms', 'biolockj/module/r/BuildPvalHistograms', 'biolockj/module/r/CalculateStats', 'biolockj/module/report/Normalizer'], myModules)


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
    saveConfigParamsForm();
  };// end toggleSelectModule

  function makeModuleLi(link, classes){

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

  var modUl = document.getElementById('module_ul');

  while (modUl.firstChild) {//clear out the old module list elements
  modUl.removeChild(modUl.firstChild);
}

  for (let mod of myModules.keys()){
    try {
      makeModuleLi(mod, myModules.get(mod).cssClass);
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

//Event listeners
 //eventlistener for adding the recent config files to "recent"
 document.getElementById("recent").addEventListener("mouseover", function() {
   recentMenuChoices = Object.keys(localStorage);
   for (var i = 0; i < recentMenuChoices.length; i++) {
     let opt = document.createElement('a');
     opt.setAttribute("name", recentMenuChoices[i]);
     var text = document.createTextNode(recentMenuChoices[i].toString());
     opt.addEventListener("click", function() {
       const configJson = JSON.parse(localStorage.getItem(this.name));
       console.log('local file');
       console.log(configJson);
       sendConfigDataToForms(configJson, configJson.modules);
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

 // Close the dropdown if the user clicks outside of it
 window.onclick = function(e) {
   if (!e.target.matches('.dropbtn')) {
     var myDropdown = document.getElementById("projects");
     if (myDropdown.classList.contains('show')) {
       myDropdown.classList.remove('show');
     }
   }
 }
