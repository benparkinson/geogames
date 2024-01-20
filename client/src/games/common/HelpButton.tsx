import React, { useRef, useState } from 'react';
import { Button, Overlay, Tooltip } from 'react-bootstrap';

interface HelpButtonProps {
    giveUp: () => void;
    openExplanationModal: () => void;
    openClueModal?: () => void;
}

const HelpButton = ({ giveUp, openExplanationModal, openClueModal }): JSX.Element => {
    const [showHelp, setShowHelp] = useState(false);
    const target = useRef(null);

    function handleExplanationClick() {
        setShowHelp(false);
        openExplanationModal();
    }

    function handleClueClick() {
        setShowHelp(false);
        openClueModal();
    }

    return (
        <div className="help-button-container">
            <Button ref={target} variant="secondary" onClick={() => setShowHelp(!showHelp)}>?</Button>
            <Overlay target={target.current} show={showHelp} placement="bottom" >
                {(props) => (
                    <Tooltip {...props}>
                        <div className="m-1 d-flex justify-content-start">
                            <Button variant="warning" onClick={giveUp} >Give up</Button>
                        </div>
                        <div className="m-1 d-flex justify-content-start">
                            <Button variant="light" onClick={handleExplanationClick} >Game explanation</Button>
                        </div>
                        {openClueModal &&
                            <div className="m-1 d-flex justify-content-start">
                                <Button variant="light" onClick={handleClueClick} >Show clue</Button>
                            </div>}
                    </Tooltip>
                )}
            </Overlay>
        </div>
    );
};

export default HelpButton;
