# TestByte — Azure setup for IT

This is a self-contained walkthrough for setting up hosting for an internal recruiting tool ("TestByte")
on the organization's Microsoft/Azure account. It doesn't assume you know anything about the app itself —
just enough Azure Portal familiarity to create three resources and copy a few values back to the person
who asked you to do this.

**Everything below stays inside one resource group**, so it's easy to see the total cost and to delete
everything cleanly later if needed. Nothing here touches any other part of the organization's Azure/M365
setup.

## What you're creating

| # | Resource | What it's for |
|---|---|---|
| 1 | A resource group | A folder that holds the other three things |
| 2 | An Azure Database for PostgreSQL (Flexible Server) | The app's database |
| 3 | An App Service (Web App), Java runtime | Runs the backend server |
| 4 | A Static Web App | Serves the web interface (what recruiters/candidates open in a browser) |

**Estimated cost: ~$28–35/month total**, billed to whatever payment method is on the Azure subscription.

## Before you start

1. **Confirm there's an Azure subscription under the organization's Microsoft tenant.** If your
   organization uses Microsoft 365 but has never used Azure before, go to
   [portal.azure.com](https://portal.azure.com), sign in with an account that has Global Administrator (or
   Billing Administrator) rights, and follow the prompt to activate a Pay-As-You-Go subscription. This
   bills through the same Microsoft account — no separate contract.
2. **Pick a region** — whichever Azure region is closest to your office, or matches wherever the
   organization's other Azure/M365 resources already run (e.g. `East US`, `Central US`, `South Central
   US`). Use the **same region for all four resources below**.
3. Have access to whoever manages DNS for the `yourdomain.com` domain — you'll need to add a couple of DNS
   records near the end. If that's you, great; if not, you'll just need to pass 2–3 records along to them
   later, no need to hand over full domain access.

---

## Step 1 — Resource group

1. In the Azure Portal, search **"Resource groups"** in the top search bar → **+ Create**.
2. Subscription: the one from "Before you start."
3. Resource group name: `testbyte-prod-rg`
4. Region: your chosen region.
5. **Review + create** → **Create**.

## Step 2 — Database (Azure Database for PostgreSQL, Flexible Server)

1. **+ Create a resource** → search **"Azure Database for PostgreSQL flexible server"** → **Create**.
2. Basics tab:
   - Resource group: `testbyte-prod-rg`
   - Server name: `testbyte-db` (must be globally unique across all of Azure — if it says taken, try
     `testbyte-db-<your-company-initials>`)
   - Region: your chosen region
   - PostgreSQL version: **17** (or the newest available if 17 isn't offered)
   - Workload type: **Development** (this steers you to the cheapest compute tier)
   - Compute + storage → **Configure server**: select **Burstable**, size **B1ms (1 vCore, 2 GiB
     memory)**. Storage: leave at the default (32 GiB) unless it lets you go lower — 32 GiB is plenty for
     this app. Turn off any auto-growth/backup-redundancy options that add cost if you're offered a
     choice — geo-redundant backup isn't needed here, locally-redundant is fine.
   - Authentication method: **PostgreSQL authentication only**
   - Admin username: pick one, e.g. `testbyte_admin` (cannot be `admin`, `azure_superuser`, `root`, or a
     few other reserved words — Azure will tell you if it's rejected)
   - Password: generate a strong password and **save it somewhere secure** — you'll send this to the
     developer along with the other values at the end.
3. Networking tab:
   - Connectivity method: **Public access**
   - Under firewall rules, check **"Allow public access from any Azure service within Azure to this
     server"**. This lets the App Service (step 3, also inside Azure) reach the database without you
     needing to manage IP allowlists.
4. **Review + create** → **Create**. This takes a few minutes.
5. Once it's done, open the resource → left menu **Databases** → **+ Add** → name it `testbyte` → Save.
   (The server itself isn't a database — this creates the actual database the app will use inside it.)
6. On the resource's **Overview** page, note the **Server name** (looks like
   `testbyte-db.postgres.database.azure.com`) — you'll need this at the end.

## Step 3 — Backend (App Service, Java)

1. **+ Create a resource** → search **"Web App"** → **Create**.
2. Basics tab:
   - Resource group: `testbyte-prod-rg`
   - Name: `testbyte-backend` (this becomes part of a default URL like
     `testbyte-backend.azurewebsites.net` — must be globally unique, add a suffix if taken)
   - Publish: **Code**
   - Runtime stack: **Java 21** — then a second dropdown appears, pick **Java SE (Embedded Web Server)**
   - Operating System: **Linux**
   - Region: your chosen region
   - Pricing plan: click **Create new** next to App Service Plan if one doesn't exist, name it anything
     (e.g. `testbyte-plan`), then change the **Sku and size** to **Basic B1**.
3. **Review + create** → **Create**.
4. Once it's created, open the resource. You'll configure two things here — application settings
   (essentially environment variables the app reads on startup) and a few features:

   **A. Turn on Always On** (this is the setting that actually removes the cold-start lag):
   - Left menu → **Configuration** → **General settings** tab → set **Always On** to **On** → **Save**.

   **B. Add application settings** — left menu → **Configuration** → **Application settings** tab →
   **+ New application setting** for each row below (name exactly as shown, value as described):

   | Name | Value |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://<server name from step 2>:5432/testbyte?sslmode=require` (replace `<server name from step 2>` with the value you noted, e.g. `jdbc:postgresql://testbyte-db.postgres.database.azure.com:5432/testbyte?sslmode=require`) |
   | `DB_USERNAME` | the admin username you set in step 2 |
   | `DB_PASSWORD` | the admin password you set in step 2 |
   | `JWT_SECRET` | any random string at least 32 characters long — you can generate one at [randomkeygen.com](https://randomkeygen.com) (use a "CodeIgniter Encryption Key"-style long string), or just mash the keyboard for 40+ characters |
   | `MAIL_HOST` | `smtp.zoho.com` |
   | `MAIL_PORT` | `587` |
   | `MAIL_USERNAME` | (leave for the developer to fill in — it's their Zoho address) |
   | `MAIL_PASSWORD` | (leave for the developer to fill in — a Zoho app password, not a login password) |
   | `MAIL_FROM` | (leave for the developer to fill in) |
   | `FRONTEND_BASE_URL` | `https://app.yourdomain.com` |
   | `WEBSITES_PORT` | `8080` |

   Click **Save** at the top after adding all of them, then **Continue** to confirm the restart.

   You're welcome to fill in the three Zoho-related rows too if you already know the values; otherwise
   leave them out for now and the developer can add them in this same screen later if they end up with
   access, or send you the three values to paste in.

5. On the resource's **Overview** page, note the **Default domain** (e.g.
   `testbyte-backend.azurewebsites.net`).

## Step 4 — Frontend (Static Web App)

1. **+ Create a resource** → search **"Static Web App"** → **Create**.
2. Basics tab:
   - Resource group: `testbyte-prod-rg`
   - Name: `testbyte-frontend`
   - Plan type: **Free**
   - Region (for the "Azure Functions" field, if shown): any nearby region, doesn't need to match the
     others — Static Web Apps don't run in a single region the way the other two do.
   - Deployment details / Source: choose **Other** (not GitHub) — this creates the resource without
     wiring it to a repo. Someone will connect GitHub Actions to it separately using the deployment token
     from the next step.
3. **Review + create** → **Create**.
4. Once created, open the resource → left menu **Overview** → **Manage deployment token** → copy the
   token shown. Treat this like a password.

## Step 5 — Custom domain (yourdomain.com)

You'll add two subdomains — `app.yourdomain.com` for the web interface and `api.yourdomain.com` for the backend.
The bare `yourdomain.com` domain itself doesn't need to change (leave existing email/website records alone).

**For the frontend (`app.yourdomain.com`):**
1. On the Static Web App resource → left menu **Custom domains** → **+ Add**.
2. Enter `app.yourdomain.com` → it will show you a CNAME record to create.
3. Go to wherever `yourdomain.com`'s DNS is managed and add that CNAME record (Host: `app`, pointing to the
   value Azure gave you).
4. Back in Azure, click **Add** to verify — usually takes a few minutes once DNS propagates. SSL is
   issued automatically once verified, no extra step.

**For the backend (`api.yourdomain.com`):**
1. On the App Service resource → left menu **Custom domains** → **+ Add custom domain**.
2. Enter `api.yourdomain.com` → Azure will show a CNAME record and a TXT record (for ownership verification).
3. Add both records at your DNS provider (the TXT record can be removed after verification succeeds; the
   CNAME stays).
4. Back in Azure, click **Validate**, then **Add custom domain**.
5. After it's added, go to **Custom domains** → next to `api.yourdomain.com`, click **Add binding** → choose
   **App Service Managed Certificate** → **Create New** → this issues a free SSL certificate for that
   domain automatically.
6. Update the `FRONTEND_BASE_URL` application setting from step 3 if it isn't already
   `https://app.yourdomain.com`.

## Step 6 — Access for the developer (pick one)

- **Option A — grant scoped access.** Left menu of the resource group `testbyte-prod-rg` →
  **Access control (IAM)** → **+ Add role assignment** → role **Contributor** → assign to the developer's
  Microsoft account (or email, if they're a guest). This lets them manage only what's inside this resource
  group, nothing else in the organization's Azure.
- **Option B — no portal access, just hand off values.** Skip this step and send everything in the
  checklist below instead. The developer can fully deploy and update the app using GitHub Actions with
  just those values — they never need to log into Azure at all.

---

## Send these values back

Whichever option you picked above, please send these back securely (not over plain email/chat if
possible — a password manager share, or split across two channels):

- [ ] **Database server name** (from step 2, e.g. `testbyte-db.postgres.database.azure.com`)
- [ ] **Database admin username and password** (from step 2) — only needed if the developer will ever
  need to inspect/fix the database directly; not needed for normal deploys if you've already filled in
  `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` as application settings in step 3.
- [ ] **Backend default URL** (from step 3, e.g. `testbyte-backend.azurewebsites.net`)
- [ ] **App Service publish profile** — App Service resource → **Overview** → **Get publish profile**
  (downloads a file). Send the contents of this file. This is what lets GitHub Actions deploy new code
  without any other Azure access.
- [ ] **Static Web App deployment token** (from step 4)
- [ ] Confirmation that `app.yourdomain.com` and `api.yourdomain.com` are both verified and showing a valid SSL
  certificate in the portal (steps 5)

That's everything needed to wire up automatic deployments from GitHub — no further portal access required
unless something needs troubleshooting later.
