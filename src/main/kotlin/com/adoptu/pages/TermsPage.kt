package com.adoptu.pages

import kotlinx.html.*

fun HTML.termsPage() {
    commonHead("Terms and Conditions - Adopt-U")
    body {
        header {
            a("/") { commonLogo() }
            nav { guestNav() }
        }
        main {
            div(classes = "policy-content") {
                h1 { attributes["data-i18n"] = "termsConditions"; +"Terms and Conditions" }
                p { +"Last updated: April 2025" }
                
                h2 { +"Acceptance of Terms" }
                p { +"By accessing and using Adopt-U, you accept and agree to be bound by the terms and provisions of this agreement. If you do not agree to these terms, please do not use our platform." }
                
                h2 { +"Nature of Our Service" }
                p { +"Adopt-U is a connection platform that facilitates contact between pet rescuers and potential adopters. We are not a pet store, broker, shelter, or adoption agency. We do not own, custody, or intermediately handle any pets listed on our platform." }
                p { +"Our role is strictly limited to providing a communication channel between parties. We do not participate in adoption negotiations, contracts, or the physical transfer of animals." }
                
                h2 { +"User Responsibilities" }
                h3 { +"For Rescuers:" }
                ul {
                    li { +"Provide accurate, truthful information about pets in your care" }
                    li { +"Ensure pets are healthy, vaccinated, and appropriately vetted before listing" }
                    li { +"Conduct responsible screening of potential adopters" }
                    li { +"Arrange safe transfer of pets to their new homes" }
                    li { +"Accept responsibility for the accuracy of all information provided" }
                    li { +"Comply with all local laws and regulations regarding pet adoption" }
                    li { +"Never use the platform for commercial pet sales or breeding purposes" }
                }
                h3 { +"For Adopters:" }
                ul {
                    li { +"Provide accurate information about your living situation and experience with pets" }
                    li { +"Be prepared to demonstrate your ability to provide proper care" }
                    li { +"Understand that rescinding a pet's adoption causes significant stress to the animal" }
                    li { +"Respect the rescuer's right to decline your adoption request" }
                }
                
                h2 { +"Pet Listings and Information" }
                p { +"Adopt-U does not verify, endorse, or guarantee the accuracy of information provided in pet listings. Information including but not limited to breed, age, health status, temperament, and vaccination records is provided by rescuers and has not been independently verified." }
                p { +"Photos and descriptions are provided by rescuers. While we prohibit deliberate misrepresentation, we cannot guarantee that all listings accurately represent the current condition of the animal." }
                p { +"Users are encouraged to ask questions, request veterinary records, and meet pets before finalizing any adoption arrangement." }
                
                h2 { +"Adoption Process" }
                p { +"Once a rescuer and adopter agree to proceed with an adoption:" }
                ul {
                    li { +"All arrangements, including transfer date, location, and any associated costs, are negotiated directly between the parties" }
                    li { +"Adopt-U does not facilitate or guarantee any payment transactions" }
                    li { +"The platform does not provide adoption contracts; any contractual arrangements are the sole responsibility of the parties involved" }
                    li { +"We strongly recommend documenting all agreements in writing" }
                }
                
                h2 { +"Limitation of Liability" }
                p { +"Adopt-U shall not be held liable for:" }
                ul {
                    li { +"Any disputes, disagreements, or conflicts between rescuers and adopters" }
                    li { +"The accuracy of information provided in pet listings" }
                    li { +"The health, behavior, or condition of any animal before, during, or after adoption" }
                    li { +"Any financial losses, damages, or injuries arising from use of the platform" }
                    li { +"The outcome of any adoption, including but not limited to returns, health issues, or compatibility problems" }
                    li { +"Any actions taken by users outside of the platform after contact has been established" }
                }
                p { +"Users acknowledge that adopting a pet is a significant commitment and that they assume full responsibility for their decision to adopt." }
                
                h2 { +"Prohibited Activities" }
                p { +"The following activities are strictly prohibited on Adopt-U:" }
                ul {
                    li { +"Commercial pet sales, breeding operations, or puppy mills" }
                    li { +"Listing pets you do not directly care for" }
                    li { +"False or misleading information in listings or profiles" }
                    li { +"Harassment, discrimination, or inappropriate contact with other users" }
                    li { +"Using the platform to collect personal information for purposes unrelated to pet adoption" }
                    li { +"Any illegal activities or violations of animal welfare laws" }
                }
                
                h2 { +"Account Termination" }
                p { +"Adopt-U reserves the right to suspend or terminate accounts that violate these terms, engage in prohibited activities, or bring the platform into disrepute. We may also remove listings that contain false, misleading, or inappropriate content." }
                
                h2 { +"Modifications to Terms" }
                p { +"We reserve the right to modify these terms at any time. Continued use of the platform after changes constitutes acceptance of the modified terms. We will notify users of significant changes via email or platform announcements." }
                
                h2 { +"Contact Us" }
                p { +"If you have any questions about these Terms and Conditions, please contact us at admin@adopt-u.com." }
            }
        }
        footer()
        commonScripts()
    }
}
