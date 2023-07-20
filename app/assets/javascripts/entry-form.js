var ready = (callback) => {
    if (document.readyState != "loading") callback();
    else document.addEventListener("DOMContentLoaded", callback);
}

ready(() => {
    function addSelectOnChangeEventHandler(select) {
        select.addEventListener("change", function() {toggleIdentityProviderFields(this.options[this.selectedIndex].value)})
    }
    function toggleIdentityProviderFields(value ) {
        var idProviderContainer = document.getElementById("identityProviderContainer")
        if(value == "LOCK") {
            idProviderContainer.style.display = 'block'
        } else {
            idProviderContainer.style.display = 'none'
        }
    }
    var actionSelect  = document.getElementById("action")
    addSelectOnChangeEventHandler(actionSelect)
    toggleIdentityProviderFields(actionSelect.options[actionSelect.selectedIndex].value)
});