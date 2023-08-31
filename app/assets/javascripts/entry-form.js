function onLoad() {
  function addSelectOnChangeEventHandler(select) {
      select.addEventListener("change", event => {
        toggleIdentityProviderFields(event.target.options[event.target.selectedIndex].value)
      });
  }

  function toggleIdentityProviderFields(value) {
      const idProviderContainer = document.getElementById("identityProviderContainer");

      if(value == "LOCK") {
          idProviderContainer.style.display = 'block';
          idProviderContainer.querySelectorAll('input').forEach( input => {
            input.disabled = false;
          });
      } else {
          idProviderContainer.style.display = 'none';
          idProviderContainer.querySelectorAll('input').forEach( input => {
            input.disabled = true;
          });
      }
  }
  const actionSelect = document.getElementById("action");
  addSelectOnChangeEventHandler(actionSelect);
  toggleIdentityProviderFields(actionSelect.options[actionSelect.selectedIndex].value);
}

if (document.readyState != "loading") onLoad();
else document.addEventListener("DOMContentLoaded", onLoad);
