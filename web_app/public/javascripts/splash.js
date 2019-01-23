var projectsViewer = document.getElementById('projectsViewer');

var handleBarsTest = {test : 'testing handlebars'}

var projects = retrieveProjects();
projects.then(proj => {
  console.log(proj);
  distributeIcons(projectsViewer, proj)
});

function distributeIcons(containerDiv, iconFileNames){
  console.log(iconFileNames);
  console.log(containerDiv);
  for (var i = 0; i < iconFileNames.length; i++) {
    const spn = document.createElement('span');
    spn.classList.add('projectIcon');
    spn.innerHTML = iconFileNames[i];

    const selecter = document.createElement('SELECT');
    const opt1 = document.createElement('option');
    const opt2 = document.createElement('option');
    opt1.innerHTML = 'View interactive visualization 1';
    opt2.innerHTML = 'View interactive visualization 2';

    selecter.appendChild(opt1);
    selecter.appendChild(opt2);
    spn.appendChild(selecter);
    containerDiv.appendChild(spn);
  }
};
function saveConfigToGui(configTextandNameObject){
  var request = new XMLHttpRequest();
  request.open('POST', '/saveConfigToGui', true);
  request.setRequestHeader("Content-Type", "application/json");
  request.send(JSON.stringify(
    configTextandNameObject
  ));
  console.log('launch sent');
  request.onreadystatechange = function() {
  if (request.readyState == XMLHttpRequest.DONE) {
    console.log(request.responseText);
    //window.location = '/progress';
    }
  }
  //return returnPromiseFromServer('/saveConfigToGui', requestParameter = configTextandNameObject);
}

function retrieveProjects(){
  return returnPromiseFromServer('/retrieveProjects', requestParameter = null);
}//end function retrieveProjects

function retrieveConfigs(){
  return returnPromiseFromServer('/retrieveConfigs', requestParameter = null);
}//end function retrieveProjects

function retrievePropertiesFile(configName){
  console.log(configName);
  return returnPromiseFromServer('/retrievePropertiesFile', requestParameter = configName);
}

// function retreiveDefaultProps(dpropPath){
//   returnPromiseFromServer('/defaultproperties', requestParameter = dpropPath)
// }

function returnPromiseFromServer(address, requestParameter, method = 'POST'){
  return new Promise((resolve, reject) => {
    var request = new XMLHttpRequest();
    request.open(method, address, true);
    request.setRequestHeader("Content-Type", "application/json");
    request.onload = function() {
      try{
        if(this.status === 200 && request.readyState === 4){
          resolve(JSON.parse(this.responseText));
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
    try {
      if (requestParameter) {
        request.send(JSON.stringify(requestParameter));
      }
      else {
        request.send();
      }
    } catch (e) {
      console.error(e);
    }
  })
}
