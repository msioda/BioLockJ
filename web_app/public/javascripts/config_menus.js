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
  urlRoot="https://raw.githubusercontent.com/msioda/BioLockJ/master/src/"
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
  //saveConfigParamsForm();
}

//Creating a Map object for module information
myModules = new Map(Object.entries({
  'biolockj/module/implicit/ImportMetadata' :
  { cssClass : ['implicit','hidden'], label : 'Metadata Importer', category : 'implicit'},

  'biolockj/module/implicit/Demultiplexer' :
  { cssClass : ['implicit', 'hidden'], category : 'implicit'},

  'biolockj/module/seq/SeqFileValidator' : { cssClass : [], category : 'seq'},

  'biolockj/module/seq/TrimPrimers' : { cssClass : [], category : 'seq'},

  'biolockj/module/seq/Rarefier' : { cssClass : [], category : 'seq'},

  'biolockj/module/classifier/r16s/QiimeClosedRefClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/r16s/RdpClassifier' :
  { cssClass : ['rdpClass', 'classifierUnique'], label : "16S Classifier: RDP", counter : 'rdpModuleCounter' , category : 'classifier'},

  'biolockj/module/implicit/parser/r16s/RdpParser' :
  { cssClass : ['rdpClass', , 'classifierUnique', 'implicit', 'hidden'], counter : 'rdpModuleCounter' , category : 'implicit.parser'},

  'biolockj/module/implicit/parser/r16s/QiimeParser' :
  { cssClass : ['rdpClass', , 'classifierUnique', 'implicit', 'hidden'], counter : 'rdpModuleCounter' , category : 'implicit.parser'},

  'biolockj/module/classifier/r16s/QiimeDeNovoClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/r16s/QiimeOpenRefClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/wgs/KrakenClassifier' :
  { cssClass : ['krakenClass', 'classifierUnique'], counter : 'krakenModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/wgs/MetaphlanClassifier' :
  { cssClass : ['metaphlanClass', 'classifierUnique'], counter : 'metaphlanModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/wgs/SlimmClassifier' :
  { cssClass : ['slimmClass', 'classifierUnique'], counter : 'slimmModuleCounter', category : 'classifier'},

  'biolockj/module/implicit/parser/wgs/KrakenParser' :
  { cssClass : ['krakenClass', 'classifierUnique'], counter : 'krakenModuleCounter', category : 'implicit.parser'},

  'biolockj/module/implicit/parser/wgs/MetaphlanParser' :
  { cssClass : ['metaphlanClass', 'classifierUnique'], counter : 'metaphlanModuleCounter', category : 'implicit.parser'},

  'biolockj/module/implicit/parser/wgs/SlimmParser' :
  { cssClass : ['slimmClass', 'classifierUnique'], counter : 'slimmModuleCounter', category : 'implicit.parser'},

  'biolockj/module/report/Normalizer' : { cssClass : [], category : 'report'},

  'biolockj/module/r/BuildMdsPlots' : { cssClass : [], category : 'r'},

  'biolockj/module/r/BuildOtuPlots' : { cssClass : [], category : 'r'},

  'biolockj/module/r/BuildPvalHistograms' : { cssClass : [], category : 'r'},

  'biolockj/module/r/CalculateStats' : { cssClass : [], category : 'r'},

  'biolockj/module/report/AddMetadataToOtuTables' : { cssClass : [], category : 'report'},

  'biolockj/module/report/JsonReport' : { cssClass : [], category : 'report'},

  'biolockj/module/seq/AwkFastaConverter' : { cssClass : [], category : 'seq'},

  'biolockj/module/seq/Multiplexer' : { cssClass : [], category : 'seq'},

  'biolockj/module/seq/PearMergeReads' : { cssClass : [], category : 'seq'},

  'biolockj/module/seq/Gunzipper' : { cssClass : [], category : 'seq'},

  'biolockj/module/implicit/RegisterNumReads' : { cssClass : ['implicit', 'hidden'], category : 'implicit'},

  'biolockj/module/report/Email' : { cssClass : [], category : 'report'},

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
  };// end toggleSelectModule

  function makeModuleLi(link, classes){
    var mod = document.createElement('li');
    for (var c = 0; c < classes.length; c++){
      mod.classList.add(classes[c])
    }//  ^this function is a hack because classList.add() adds commas between the css classes, making them unreadable to css
    mod.setAttribute('draggable', true);
    mod.innerHTML = link.split('.')[0].replace(/\//g,'.');//remove .java then replace / with .
    var text = getText(getUrl(link));
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

 // Close the dropdown if the user clicks outside of it
 window.onclick = function(e) {
   if (!e.target.matches('.dropbtn')) {
     var myDropdown = document.getElementById("projects");
     if (myDropdown.classList.contains('show')) {
       myDropdown.classList.remove('show');
     }
   }
 }
