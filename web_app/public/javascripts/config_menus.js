/* When the user clicks on the "Projects"button,
            toggle between hiding and showing the dropdown content */
function projectOptions() {
  document.getElementById("projects").classList.toggle("show");
}

function toggleShow(trgt, useId = true) {
//can be used on html objects or their ids
  let x;
  if (useId = true){
    x = document.getElementById(trgt);
  }else{
    x = trgt;
  }
  if (x.style.display === "none") {
    x.style.display = "block";
  } else {
    x.style.display = "none";
  }
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
  runModuleFunctions();
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
document.getElementById('openAwsSettings').addEventListener('click', function() {
  document.getElementById('awsSettings').style.display = "block";
})

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
  console.assert(typeof bljModuleJavaClassName === "string", "getUrl function requires string argumentd");
  urlRoot="https://raw.githubusercontent.com/msioda/BioLockJ/master/src/";
  return (urlRoot + bljModuleJavaClassName + '.java');
}//end getUrl
function moduleLiHoverTextfromJavadocs(moduleJavaClassPath){
  return new Promise((resolve, reject) => {
    var request = new XMLHttpRequest();
    request.open('POST', '/javadocsmodulegetter', true);
    request.setRequestHeader("Content-Type", "application/json");
    request.onload = function() {
      try{
        if(this.status === 200 && request.readyState === 4){
          resolve(this.responseText);
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
    request.send(JSON.stringify({
      moduleJavaClassPath : moduleJavaClassPath,
    }));
  })
}//end moduleLiHoverTextfromJavadocs

function parseBljModuleJavaClass(text){//gets text java documentation from Java class
  let startPublicClass;
  let startComment;
  let lines = text.split('\n');
  let modDescrip = ""
  let webDesc;
 for (let i = 0; i < lines.length; i++){
   if (lines[i].startsWith("public class")){
     startPublicClass = i;
   }
 }
 for (let i = startPublicClass; i > 0; i--) {
   if (lines[i].startsWith("/**") && lines[i+1].startsWith(' * This ')) {
     startComment = i;
   }
 }
 for (var a = startComment; a < startPublicClass; a++){
   var sect = lines[a].slice(3,);
   modDescrip = modDescrip.concat(sect);
 }
 if (modDescrip.includes('@blj.web_desc')){
   let split = modDescrip.split('@blj.web_desc ');
   modDescrip = split[0];
   webDesc = split[1]
   return [modDescrip, webDesc];
 }
 return [modDescrip];
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
    if (!target.classList.contains("modChoosen")) {
      // target.classList.remove("modChoosen");
      this.decrementCount();
      if (this.getCount() == 0) {
        for (let t = 0; t < this.modsToDisable.length; t++) {
          removeClassToAllElemInList(this.modsToDisable[t], "disabledMod");
        }
      }
    }else {
      // target.classList.add("modChoosen");
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
let myModules = new Map(Object.entries({
  'biolockj/module/implicit/ImportMetadata' :
  { cssClass : ['implicit','hidden'], label : 'Metadata Importer', category : 'implicit'},

  'biolockj/module/implicit/Demultiplexer' :
  { cssClass : ['implicit', 'hidden'], category : 'implicit'},

  'biolockj/module/seq/SeqFileValidator' : { cssClass : ['SeqFileValidatorMod'], category : 'seq'},

  'biolockj/module/seq/TrimPrimers' : { cssClass : [], category : 'seq'},

  'biolockj/module/classifier/r16s/QiimeClosedRefClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/wgs/Humann2Classifier' :
  { cssClass : ['humann2Class', 'classifierUnique'], label : "WGS Classifier: HumanN2", counter : 'humann2ModuleCounter' , category : 'classifier'},

  'biolockj/module/classifier/r16s/RdpClassifier' :
  { cssClass : ['rdpClass', 'classifierUnique'], label : "16S Classifier: RDP", counter : 'rdpModuleCounter' , category : 'classifier'},

  'biolockj/module/implicit/parser/r16s/RdpParser' :
  { cssClass : ['rdpClass', 'classifierUnique', 'implicit', 'hidden'], counter : 'rdpModuleCounter' , category : 'implicit.parser'},

  'biolockj/module/implicit/parser/r16s/QiimeParser' :
  { cssClass : ['rdpClass', 'classifierUnique', 'implicit', 'hidden'], counter : 'rdpModuleCounter' , category : 'implicit.parser'},

  'biolockj/module/classifier/r16s/QiimeDeNovoClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/r16s/QiimeOpenRefClassifier' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter', category : 'classifier'},

  'biolockj/module/implicit/parser/wgs/Humann2Parser' :
  { cssClass : ['humann2Class', 'classifierUnique', 'implicit', 'hidden'], counter : 'humann2ModuleCounter' , category : 'implicit.parser'},

  'biolockj/module/classifier/wgs/KrakenClassifier' :
  { cssClass : ['krakenClass', 'classifierUnique'], counter : 'krakenModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/wgs/Kraken2Classifier' :
  { cssClass : ['kraken2Class', 'classifierUnique'], counter : 'kraken2ModuleCounter', category : 'classifier'},

  'biolockj/module/classifier/wgs/Metaphlan2Classifier' :
  { cssClass : ['metaphlanClass', 'classifierUnique'], counter : 'metaphlanModuleCounter', category : 'classifier'},

  // 'biolockj/module/classifier/wgs/SlimmClassifier' :
  // { cssClass : ['slimmClass', 'classifierUnique'], counter : 'slimmModuleCounter', category : 'classifier'},

  'biolockj/module/implicit/parser/wgs/KrakenParser' :
  { cssClass : ['krakenClass', 'classifierUnique', 'implicit', 'hidden'], counter : 'krakenModuleCounter', category : 'implicit.parser'},

  'biolockj/module/implicit/parser/wgs/Kraken2Parser' :
  { cssClass : ['kraken2Class', 'classifierUnique','implicit', 'hidden'], counter : 'kraken2ModuleCounter', category : 'implicit.parser'},

  'biolockj/module/implicit/parser/wgs/Metaphlan2Parser' :
  { cssClass : ['metaphlanClass', 'classifierUnique','implicit', 'hidden'], counter : 'metaphlanModuleCounter', category : 'implicit.parser'},

  // 'biolockj/module/implicit/parser/wgs/SlimmParser' :
  // { cssClass : ['slimmClass', 'classifierUnique','implicit', 'hidden'], counter : 'slimmModuleCounter', category : 'implicit.parser'},

  'biolockj/module/implicit/qiime/BuildQiimeMapping' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter', category : 'qiime'},

  'biolockj/module/implicit/qiime/MergeQiimeOtuTables' :
  { cssClass : ['qiimeClass', 'classifierUnique'], counter : 'qiimeModuleCounter', category : 'qiime'},

  'biolockj/module/report/r/R_PlotMds' : { cssClass : ['rMod', 'R_PlotMds'], category : 'r'},

  'biolockj/module/report/r/R_PlotOtus' : { cssClass : ['rMod', 'R_PlotOtus'], category : 'r'},

  'biolockj/module/report/r/R_PlotPvalHistograms' : { cssClass : ['rMod'], category : 'r'},

  'biolockj/module/report/r/R_PlotEffectSize' : { cssClass : ['rMod', 'R_PlotEffectSize'], category : 'r'},

  'biolockj/module/report/r/R_CalculateStats' : { cssClass : ['rMod', 'R_CalculateStats'], category : 'r'},

  'biolockj/module/report/taxa/AddMetadataToTaxaTables' : { cssClass : [], category : 'report'},

  'biolockj/module/report/taxa/BuildTaxaTables' : { cssClass : [], category : 'report'},

  'biolockj/module/report/taxa/NormalizeTaxaTables' : { cssClass : [], category : 'report'},

  'biolockj/module/report/otu/CompileOtuCounts' : { cssClass : [], category : 'report'},

  'biolockj/module/report/JsonReport' : { cssClass : [], category : 'report'},

  'biolockj/module/report/otu/RemoveLowOtuCounts' : { cssClass : [], category : 'report'},

  'biolockj/module/report/otu/RemoveScarceOtuCounts' : { cssClass : [], category : 'report'},

  'biolockj/module/report/otu/RarefyOtuCounts' : { cssClass : ['rarefyOtuCounts'], category : 'report'},

  'biolockj/module/report/taxa/LogTransformTaxaTables' : { cssClass : ['removeLowOtuCounts'], category : 'report'},

  'biolockj/module/seq/AwkFastaConverter' : { cssClass : [], category : 'seq'},

  'biolockj/module/seq/KneadData' : { cssClass : ['kneadDataClass'], category : 'seq'},

  'biolockj/module/seq/Multiplexer' : { cssClass : [], category : 'seq'},

  'biolockj/module/seq/PearMergeReads' : { cssClass : [], category : 'seq'},

  'biolockj/module/seq/RarefySeqs' : { cssClass : ['rarefySeqs'], category : 'seq'},

  'biolockj/module/seq/Gunzipper' : { cssClass : [], category : 'seq'},

  'biolockj/module/implicit/RegisterNumReads' : { cssClass : ['implicit', 'hidden'], category : 'implicit'},

  'biolockj/module/report/Email' : { cssClass : ['emailMod'], category : 'report'},

}));

//reorder moduleLinkAndClass to suit config form
function orderModulesFromLocalFiles(selectedModulesArray, defaultOrderMap){
  if (selectedModulesArray == undefined){
    return defaultOrderMap;
  }

  //check for unknown modules
  for (let mod  of selectedModulesArray){
    if (!Array.from(defaultOrderMap.keys()).includes( mod.replace( /\./g,'/' ) )){
    alert('Unknown modules in local file: ' + mod);
    console.error('Unknown modules in local file: ' + mod);
    break;
    }
  }

  const selectedModulesArraySet = new Set(selectedModulesArray);

  if (selectedModulesArray.length > selectedModulesArraySet.size){
    alert("There are duplicate modules in your local file.")
    return false;
  }

  let reorderedMods = new Map();

  while (defaultOrderMap.size > 0){
    for (let mod of defaultOrderMap.keys()){
      const modPeriod = mod.replace( /\//g,'.' );
      if (!selectedModulesArray.includes(modPeriod)){
        reorderedMods.set(mod, defaultOrderMap.get(mod));
        defaultOrderMap.delete(mod);
      }else if (modPeriod == selectedModulesArray[0]){
        reorderedMods.set(mod, defaultOrderMap.get(mod));
        defaultOrderMap.delete(mod);
        selectedModulesArray.shift();
      }
    }//end for
  }//end while

  console.log(reorderedMods);
  return reorderedMods;
}//end orderModulesFromLocalFiles
//let test = orderModulesFromLocalFiles(['biolockj/module/r/BuildPvalHistograms', 'biolockj/module/r/BuildPvalHistograms', 'biolockj/module/r/CalculateStats', 'biolockj/module/report/Normalizer'], myModules)


function runModuleFunctions() {//large function to build module li and counters

  function toggleSelectModule(target) {//function called when modules are selected, it both selects them and disables others
    //first, enable or disable the mods, then add or remove their button from the tab buttons
    const tabButtons = document.getElementById('tabButtons');
    if (target.classList.contains("modChoosen")){
      target.classList.remove("modChoosen");
      //remove the module's tab button
      for (let i = 0; i < target.classList.length; i++) {
        let thisClassMenu = tabButtons.getElementsByClassName(target.classList[i]);
        if (thisClassMenu.length > 0) {
          for (let i = 0; i < thisClassMenu.length; i++) {
            thisClassMenu[i].style.display = 'none';
            thisClassMenu[i].classList.remove('modChoosen');
          }//end second for loop
        }
      }//End first for loop
    }else{
      target.classList.add("modChoosen");
      //add the module's tab to tab Buttons
      for (let i = 0; i < target.classList.length; i++) {
        let thisClassMenu = tabButtons.getElementsByClassName(target.classList[i]);
        if (thisClassMenu.length > 0) {
          for (let i = 0; i < thisClassMenu.length; i++) {
            thisClassMenu[i].style.display = 'block';
            thisClassMenu[i].classList.add('modChoosen');
          }//end second for loop
        }
      }//first for loop
    }
    //Next, let module counters calculate if any modules need to be disabled or enabled ect
    if (target.classList.contains("qiimeClass")) {
      qiimeModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("rdpClass")) {
      rdpModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("krakenClass")) {
      krakenModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("kraken2Class")) {
      kraken2ModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("slimmClass")) {
      slimmModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("metaphlanClass")) {
      metaphlanModuleCounter.modClassSelected(target)
    }else if (target.classList.contains("humann2Class")) {
      humann2ModuleCounter.modClassSelected(target)
    }
  };// end toggleSelectModule

  function makeModuleLi(link, classes){
    // console.log('link: ', link);
    try {
      let mod = document.createElement('li');
      for (var c = 0; c < classes.length; c++){
        mod.classList.add(classes[c])
      }//  ^this function is a hack because classList.add() adds commas between the css classes, making them unreadable to css
      mod.setAttribute('draggable', true);
      mod.setAttribute('data-link', link.replace(/\//g,'.'));
      mod.innerHTML = modNameFromLinkNoCamelCase(link);
      try {
        let text = moduleLiHoverTextfromJavadocs(link);
        //console.log('makeModuleLi link: ', text);
        text.then(result => {
          let parsedResult = parseBljModuleJavaClass(result);
          //console.log('result: ', result);
          // console.log('parsedResult[0]: ', parsedResult[0]);
          mod.setAttribute('data-info', parsedResult[0]);
          hoverEventlistenerForModules(mod);
          if (parsedResult.length > 1){
            mod.innerHTML = parsedResult[1];
          }
          });
      } catch (e) {
        console.error(e);
      }
      mod.addEventListener('dragstart', function(){dragStarted(event)});
      mod.addEventListener('dragover', function(){draggingOver(event)});
      mod.addEventListener('drop',function(){dropped(event)});
      mod.addEventListener('click', function(){toggleSelectModule(event.target)})
      modUl.appendChild(mod);
    } catch (e) {
      console.error(e);
    }
    function modNameFromLinkNoCamelCase(link) {
      const temp = link;
      if (link.startsWith('biolockj/module/')) {
        link = link.slice(16).split('/').pop();//remove 'biolockj/module/' and then remove the 'seq,', 'report', etc.
      }
      const noCamel = link.split(/(?=[A-Z])/).join(' ');
      //console.log(temp, '\t', link, '\t', noCamel, '\n');
      // makingModList += `${temp}\t${link}\t${noCamel}\n`
      return noCamel;
    }
  };//end makeModuleLi

  var modUl = document.getElementById('module_ul');

  while (modUl.firstChild) {//clear out the old module list elements
  modUl.removeChild(modUl.firstChild);
}
  for (let mod of myModules.keys()){
    try {
      //console.log('myModules.get(mod).cssClass): ', mod, myModules.get(mod).cssClass);
      makeModuleLi(mod, myModules.get(mod).cssClass);
    } catch (e) {
      console.error(e);
    }
  }//end forloop

  const module = document.getElementById('module');
  //the following are list of module nodes with class for building the subsequence "Choosen" lists
  var qiimeClassModNodes = Array.from(module.getElementsByClassName("qiimeClass"));
  var slimmClassModNodes = Array.from(module.getElementsByClassName("slimmClass"));
  var krakenClassModNodes = Array.from(module.getElementsByClassName("krakenClass"));
  var kraken2ClassModNodes = Array.from(module.getElementsByClassName("kraken2Class"));
  var rdpClassModNodes = Array.from(module.getElementsByClassName("rdpClass"));
  var metaphlanClassModNodes = Array.from(module.getElementsByClassName("metaphlanClass"));
  var humann2ClassModNodes = Array.from(module.getElementsByClassName("humann2Class"));

  //moduleCounters instanciated with nodes that they will disable
  var qiimeModuleCounter = new moduleCounter([slimmClassModNodes, krakenClassModNodes, rdpClassModNodes, metaphlanClassModNodes, kraken2ClassModNodes, humann2ClassModNodes]);
  var rdpModuleCounter = new moduleCounter([slimmClassModNodes, krakenClassModNodes, qiimeClassModNodes, metaphlanClassModNodes, kraken2ClassModNodes, humann2ClassModNodes]);
  var krakenModuleCounter = new moduleCounter([slimmClassModNodes, qiimeClassModNodes, rdpClassModNodes, metaphlanClassModNodes, kraken2ClassModNodes, humann2ClassModNodes]);
  var kraken2ModuleCounter = new moduleCounter([slimmClassModNodes, qiimeClassModNodes, rdpClassModNodes, metaphlanClassModNodes, krakenClassModNodes, humann2ClassModNodes]);
  var slimmModuleCounter = new moduleCounter([qiimeClassModNodes, krakenClassModNodes, rdpClassModNodes, metaphlanClassModNodes, kraken2ClassModNodes, humann2ClassModNodes]);
  var metaphlanModuleCounter = new moduleCounter([slimmClassModNodes, krakenClassModNodes, rdpClassModNodes, qiimeClassModNodes, kraken2ClassModNodes, humann2ClassModNodes]);
  var humann2ModuleCounter = new moduleCounter([slimmClassModNodes, krakenClassModNodes, rdpClassModNodes, qiimeClassModNodes, kraken2ClassModNodes, metaphlanClassModNodes]);

  document.getElementById('pipeline.allowImplicitModules').addEventListener('change', function(evt){
    const implicits = Array.from(document.getElementsByClassName('implicit'));
    if (this.value === 'Y'){ implicits.forEach(imp => {imp.classList.remove('hidden')});
    }else{implicits.forEach(imp => imp.classList.add('hidden'));}
  });
};//end runModuleFunctions

 // Close the dropdown if the user clicks outside of it
 window.onclick = function(e) {
   if (!e.target.matches('.dropbtn')) {
     var myDropdown = document.getElementById("projects");
     if (myDropdown.classList.contains('show')) {
       myDropdown.classList.remove('show');
     }
   }
 }

function clearChildren(t) {
  while (t.firstChild) {
    t.removeChild(t.firstChild);
  }
}


