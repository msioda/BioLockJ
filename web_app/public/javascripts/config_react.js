/**
Author: Aaron Yerke
Purpose:
  Add the ability to select previously run pipeline for direct editing or
  cloning using the React.js framework.
Notes:
  While I want to implement React.js into this project, I might just use this for
  dev experimentation because it doesn't make sense to add a new framework into
  this project at this stage.
*/

console.log('reading react');

class ModalPanel extends React.Component {
  render() {
    return (
      <div className="modal">
        <div className="modal-content">
          <span className="closeModal">&times;</span>
          testing
        </div>
      </div>
    );
  }
}

ReactDOM.render(
  <ModalPanel />,
  document.getElementById('root')
);

